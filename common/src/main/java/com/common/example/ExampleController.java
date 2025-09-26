package com.common.example;

import com.common.exception.GlobalException;
import com.common.exception.GlobalErrorCode;
import com.common.response.DataResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/example")
public class ExampleController {

    @GetMapping("/success")
    public ResponseEntity<DataResponse<String>> getSuccess() {
        return ResponseEntity.ok(DataResponse.from("Hello World"));
    }

    @GetMapping("/success-simple")
    public ResponseEntity<DataResponse<Void>> getSuccessSimple() {
        return ResponseEntity.ok(DataResponse.ok());
    }

    @PostMapping("/create")
    public ResponseEntity<DataResponse<ExampleData>> createData(@Valid @RequestBody ExampleRequest request) {
        ExampleData data = new ExampleData(1L, request.getName());
        return ResponseEntity.status(201).body(DataResponse.from(data));
    }

    @GetMapping("/business-error")
    public ResponseEntity<DataResponse<Void>> getBusinessError() {
        throw GlobalException.from(GlobalErrorCode.BAD_REQUEST);
    }

    @GetMapping("/not-found-error")
    public ResponseEntity<DataResponse<Void>> getNotFoundError() {
        throw GlobalException.from(GlobalErrorCode.RESOURCE_NOT_FOUND);
    }

    @GetMapping("/runtime-error")
    public ResponseEntity<DataResponse<Void>> getRuntimeError() {
        throw new RuntimeException("예상치 못한 오류가 발생했습니다.");
    }

    public static class ExampleRequest {
        @NotBlank(message = "이름은 필수입니다.")
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class ExampleData {
        private Long id;
        private String name;

        public ExampleData(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}