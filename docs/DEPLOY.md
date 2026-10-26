# HƯỚNG DẪN TRIỂN KHAI PRODUCTION (VPS + Docker + Nginx + HTTPS)
**Thẻ:** OPS-03, OPS-04, OPS-05, OPS-06 · **Người chính:** Vinh · **Cập nhật:** 26/10/2026

Stack production: `db` (MySQL 8) – `app` (Spring Boot jar trong image) – `web` (nginx tĩnh) – `edge` (nginx reverse proxy + HTTPS).
Toàn bộ chạy bằng `docker-compose.prod.yml`, image lấy từ GHCR, **không build source trên server**.

---

## 0. Việc của leader (chỉ làm một lần, trên GitHub)

Vào **Settings → Secrets and variables → Actions**:

| Loại | Tên | Giá trị |
|---|---|---|
| Repository variable | `DEPLOY_ENABLED` | `true` (để `false` thì job deploy tự bỏ qua) |
| Secret | `DEPLOY_HOST` | IP hoặc domain VPS |
| Secret | `DEPLOY_USER` | user deploy không phải root (ví dụ `ffa` hoặc `deploy`) |
| Secret | `DEPLOY_SSH_KEY` | **private key** của cặp key dùng để SSH vào VPS |

Tạo key trên máy bạn (không ai phải thấy private key, kể cả nhóm):
```bash
ssh-keygen -t ed25519 -f ~/.ssh/ffa_deploy -C "gha-deploy"
# public key (~/.ssh/ffa_deploy.pub) -> thêm vào ~/.ssh/authorized_keys trên VPS
# private key (~/.ssh/ffa_deploy)     -> dán vào secret DEPLOY_SSH_KEY
```
Private key **không** commit, **không** gửi qua chat/mail (rủi ro R04 trong sheet Rủi ro).

---

## 1. Chuẩn bị VPS (Ubuntu 22.04/24.04)

```bash
# 1.1 Cập nhật + Docker Engine + Compose plugin
sudo apt update && sudo apt -y upgrade
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER && newgrp docker

# 1.2 User deploy (không dùng root để chạy app)
sudo adduser --disabled-password --gecos "" deploy
sudo usermod -aG docker deploy

# 1.3 Thư mục dự án
sudo mkdir -p /opt/ffa && sudo chown deploy:deploy /opt/ffa
cd /opt/ffa

# 1.4 Tường lửa: chỉ mở SSH + web
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
sudo ufw status verbose
# KHÔNG mở 3306, 8080, 8091 ra internet — db chỉ đi qua network nội bộ compose
```

## 2. Cấu hình lần đầu trên VPS

```bash
cd /opt/ffa
# 2.1 Lấy file deploy
git clone https://github.com/CJustin23/ProjectIT206.git .   # hoặc scp các file compose/deploy

# 2.2 Đăng nhập GHCR bằng PAT loại read:packages (token của thành viên có quyền repo)
echo "PAT_READ_PACKAGES" | docker login ghcr.io -u TAI_KHOAN --password-stdin

# 2.3 Tạo biến môi trường production
cp deploy/env.prod.example .env.prod
nano .env.prod          # đổi MYSQL_PASSWORD, MYSQL_ROOT_PASSWORD, JWT_SECRET, FFA_DOMAIN
chmod 600 .env.prod

# 2.4 Chỉ số domain vào file env để nginx và certbot dùng
export FFA_DOMAIN=$(grep '^FFA_DOMAIN=' .env.prod | cut -d= -f2)
echo $FFA_DOMAIN
```

**Domain trỏ DNS:** thêm A record `ffa.example.com → IP VPS`, chờ `dig +short ffa.example.com` trả đúng IP rồi mới làm certificate.

## 3. Bật HTTPS (certbot, standalone lúc chưa có edge)

```bash
sudo apt install -y certbot
sudo certbot certonly --standalone -d $FFA_DOMAIN \
  --pre-hook "docker stop ffa-edge || true" \
  --post-hook "docker start ffa-edge || true"
# Certificate nằm ở /etc/letsencrypt/live/$FFA_DOMAIN/ — edge mount thư mục này
sudo mkdir -p /var/www/certbot            # cho ACME webroot
# Renew tự động:
sudo crontab -l | grep -q certbot || \
  (echo "0 3 * * 1 certbot renew --quiet --deploy-hook 'docker restart ffa-edge'" | sudo crontab -)
```
Trong `deploy/nginx.ffa.conf`, thay `ffa.example.com` (4 chỗ) bằng domain thật.

## 4. Deploy

```bash
cd /opt/ffa
docker compose -f docker-compose.prod.yml --env-file .env.prod pull
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d
docker compose -f docker-compose.prod.yml --env-file .env.prod ps
curl -fsS https://$FFA_DOMAIN/api/health
```
Lần khởi động đầu tiên: Flyway tự chạy `V1__init_schema.sql` rồi `V2__seed_demo.sql`.
**Xoá dữ liệu demo trước khi dùng thật** (không có user `an@ffa.dev` mật khẩu `password` trên production):
```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod exec db \
  mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" \
  -e "delete from transactions; delete from goal_contributions; delete from budgets; delete from savings_goals; delete from refresh_tokens; delete from users where email like '%@ffa.dev';"
```

## 5. Deploy tự động bằng GitHub Actions

```bash
git tag v0.2.0 && git push origin v0.2.0      # 1) workflow "release" build + push GHCR
# 2) Actions → "Deploy" → Run workflow → nhập version = v0.2.0
```
Job deploy: `pull` đúng tag → `up -d` → smoke test `https://domain/api/health` phải trả `"connected":true`.

### Rollback
```bash
cd /opt/ffa
sed -i 's#^APP_VERSION=.*#APP_VERSION=v0.1.0#' .env.prod
sed -i 's#\(ffa-api:\).*#\1v0.1.0#; s#\(ffa-web:\).*#\1v0.1.0#' .env.prod
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d
```

## 6. Backup và restore (OPS-05)

```bash
# Cron 02:00 hằng ngày, giữ 14 bản:
sudo -u deploy crontab -l | grep -q backup-prod || \
  (echo "0 2 * * * /opt/ffa/scripts/backup-prod.sh >> /var/log/ffa-backup.log 2>&1" | sudo -u deploy crontab -)
ls -lh /opt/ffa/backups
```
Restore một bản dump:
```bash
cd /opt/ffa
gunzip -c backups/ffa-20261101-020001.sql.gz | \
  docker compose -f docker-compose.prod.yml --env-file .env.prod exec -T db \
  sh -c 'exec mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"'
docker restart ffa-app && curl -fsS https://$FFA_DOMAIN/api/health
```
Nên copy bản dump ra ngoài VPS (Google Drive/S3) — VPS chết thì backup cũng chết.

## 7. Theo dõi (OPS-06)

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod logs --tail=100 app
docker stats --no-stream
curl -fsS https://$FFA_DOMAIN/api/health | jq .
# xem container nào khởi động lại liên tục:
docker ps --format 'table {{.Names}}\t{{.Status}}'
```
`restart: unless-stopped` + healthcheck giúp app tự hồi phục; log container giới hạn bằng
`docker compose ... logs` (không ghi file trong image).

## 8. Checklist nghiệm thu triển khai (dán ảnh vào báo cáo cuối kỳ)
| # | Nội dung | Bằng chứng cần nộp |
|---|---|---|
| 1 | `docker compose ps` 4 service `Up (healthy)` | ảnh terminal |
| 2 | HTTPS hoạt động, A+ trên SSL Labs | ảnh `https://domain` + nháp điểm |
| 3 | `api/health` trả `"connected":true` | ảnh |
| 4 | Run workflow "release" xanh, image hiện trên GHCR | ảnh packages |
| 5 | Run "Deploy" xanh + smoke pass | ảnh |
| 6 | `backups/ffa-*.sql.gz` tồn tại và restore được | ảnh |
| 7 | `ufw status` chỉ 22/80/443 | ảnh |
| 8 | Không có `.env.prod` hay private key trong repo | `git log --all -p -S "JWT_SECRET="` rỗng |

## 9. Lỗi hay gặp
| Triệu chứng | Nguyên nhân | Cách xử lý |
|---|---|---|
| `pull access denied for ghcr.io/...` | VPS chưa `docker login ghcr.io` hoặc PAT thiếu `read:packages` | login lại bằng PAT mới |
| App restart loop, log `Public Key Retrieval is not allowed` | thiếu `allowPublicKeyRetrieval=true` | đã có trong JDBC url, kiểm tra lại `DB_HOST=db` |
| `404 /api/health` qua HTTPS nhưng 8090 trực tiếp OK | nginx `proxy_pass` sai upstream name | service trong compose tên `app`, không phải `ffa-app` |
| certbot `Could not bind TCP port 80` | `edge` đang giữ port | dùng `--pre-hook "docker stop ffa-edge"` như mục 3 |
| Flyway báo `Validate failed: Detected resolved migration not applied` | sửa V1/V2 sau khi đã chạy | không sửa migration đã áp dụng; tạo V3; hoặc DB mới hẳn |
