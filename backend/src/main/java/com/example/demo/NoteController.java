package com.example.demo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class NoteController {

    private final NoteRepository repository;

    public NoteController(NoteRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "ok");
        body.put("notes", repository.count());
        body.put("time", LocalDateTime.now().toString());
        return body;
    }

    @GetMapping("/notes")
    public List<Note> list() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    @PostMapping("/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public Note create(@RequestBody @Valid CreateNoteRequest request) {
        Note note = new Note();
        note.setContent(request.content());
        return repository.save(note);
    }

    @DeleteMapping("/notes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "note not found");
        }
        repository.deleteById(id);
    }

    public record CreateNoteRequest(
            @NotBlank(message = "content 不能为空")
            @Size(max = 500, message = "content 最多 500 字")
            String content) {
    }
}
