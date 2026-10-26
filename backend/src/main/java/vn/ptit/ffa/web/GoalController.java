package vn.ptit.ffa.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.ptit.ffa.security.CurrentUser;
import vn.ptit.ffa.service.GoalService;
import vn.ptit.ffa.web.dto.GoalDto;

import java.util.List;

@RestController
@RequestMapping("/api/v1/goals")
public class GoalController {

    private final GoalService goals;
    private final CurrentUser currentUser;

    public GoalController(GoalService goals, CurrentUser currentUser) {
        this.goals = goals;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<GoalDto.View> list(@RequestParam(required = false) String status) {
        return goals.list(currentUser.require(), status);
    }

    @PostMapping
    public ResponseEntity<GoalDto.View> create(@Valid @RequestBody GoalDto.Request request) {
        currentUser.requireWriter();
        return ResponseEntity.status(HttpStatus.CREATED).body(goals.create(currentUser.require(), request));
    }

    @PatchMapping("/{id}")
    public GoalDto.View update(@PathVariable Long id, @Valid @RequestBody GoalDto.Request request) {
        currentUser.requireWriter();
        return goals.update(currentUser.require(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        currentUser.requireManager();
        goals.delete(currentUser.require(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/contributions")
    public ResponseEntity<GoalDto.View> contribute(@PathVariable Long id,
                                                   @Valid @RequestBody GoalDto.ContributionRequest request) {
        currentUser.requireWriter();
        return ResponseEntity.status(HttpStatus.CREATED).body(goals.contribute(currentUser.require(), id, request));
    }
}
