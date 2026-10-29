package com.mpi.demo.service;

import java.util.Map;

import org.springframework.data.domain.Pageable;

import com.mpi.demo.dto.request.CreateUserRequest;
import com.mpi.demo.dto.request.UpdateUserRequest;
import com.mpi.demo.dto.request.UserSearchRequest;
import com.mpi.demo.dto.response.UserResponse;
import com.mpi.demo.helper.ResultPagination;

public interface UserService {

    UserResponse create(CreateUserRequest request);

    UserResponse update(UpdateUserRequest request);

    UserResponse getById(Long id);

    ResultPagination search(UserSearchRequest search, Pageable pageable);

    void delete(Long id);

    Map<String, Long> getUserCounts();
}
