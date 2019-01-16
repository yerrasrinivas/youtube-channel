package com.youtube.test;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="UPLOADED_VIDEO")
public class UploadedVideo {

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="video_id")
	private long id;
	
	@Column(name="video_name")
	private String videoName;
	
	@Column(name="link")
	private String link;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getVideoName() {
		return videoName;
	}

	public void setVideoName(String videoName) {
		this.videoName = videoName;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}
	
}
