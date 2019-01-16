package com.youtube.test;

import org.springframework.data.jpa.repository.JpaRepository;


public interface UploadedVideoRepo extends JpaRepository<UploadedVideo, Long> {

}
