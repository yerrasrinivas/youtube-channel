package com.youtube.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemListResponse;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatus;
import com.restfb.BinaryAttachment;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.FacebookClient.AccessToken;
import com.restfb.Version;
import com.restfb.types.GraphResponse;
import com.restfb.types.Page;
import com.restfb.types.ResumableUploadStartResponse;
import com.restfb.types.ResumableUploadTransferResponse;

@Controller
public class FileUploadController {

	@Autowired
	private YouTube youtube;

	@Autowired
	private UploadedVideoRepo videoRepo;

	@Autowired
	private AccessToken accessToken;

	@GetMapping("/files")
	public String getListFiles(Model model) throws IOException {
		List<PlaylistItem> playlistItemList = new ArrayList<PlaylistItem>();
		com.google.api.services.youtube.YouTube.Channels.List channelReq = youtube.channels().list("contentDetails")
				.setMine(true).setFields("items/contentDetails,nextPageToken,pageInfo");
		ChannelListResponse resp = channelReq.execute();
		List<Channel> channelList = resp.getItems();
		for (Channel channel : channelList) {

			String uploadPlaylistId = channel.getContentDetails().getRelatedPlaylists().getUploads();

			YouTube.PlaylistItems.List playlistItemRequest = youtube.playlistItems().list("id,contentDetails,snippet");
			playlistItemRequest.setPlaylistId(uploadPlaylistId);

			playlistItemRequest.setFields(
					"items(contentDetails/videoId,snippet/title,snippet/publishedAt),nextPageToken,pageInfo");

			String nextToken = "";

			do {
				playlistItemRequest.setPageToken(nextToken);
				PlaylistItemListResponse playlistItemResult = playlistItemRequest.execute();

				playlistItemList.addAll(playlistItemResult.getItems());

				nextToken = playlistItemResult.getNextPageToken();
			} while (nextToken != null);

			// Prints information about the results.
			prettyPrint(playlistItemList.size(), playlistItemList.iterator());
		}

		model.addAttribute("files", playlistItemList);
		return "listfiles";
	}

	@GetMapping("/files/upload")
	public String getFiles() {
		return "upload";
	}

	@PostMapping("/files/upload")
	public String uploadMultipartFile(@RequestParam("files") MultipartFile[] files, Model model) {
		List<String> fileNames = new ArrayList<String>();
		try {

			for (MultipartFile file : files) {
				System.out.println("::: File Name::::" + file.getOriginalFilename());
				fileNames.add(file.getOriginalFilename());

				//Upload to Youtube
				String youtubeId = uploadVideoToYoutube(file);
				
				//upload to FB
				uploadFB(file);
				
				//persist in DB
				UploadedVideo video = new UploadedVideo();
				video.setVideoName(file.getOriginalFilename());
				video.setLink("https://www.youtube.com/watch?v=" + youtubeId);
				videoRepo.save(video);

			}

			model.addAttribute("message", "Files uploaded successfully!");
			model.addAttribute("files", fileNames);
		} catch (Exception e) {
			model.addAttribute("message", "Fail!");
			model.addAttribute("files", fileNames);
		}

		return "upload";
	}

	public void test() throws IOException {
		FacebookClient client = new DefaultFacebookClient(accessToken.getAccessToken(), Version.LATEST);
		Page page = client.fetchObject("271725233519430", Page.class, com.restfb.Parameter.with("fields", "fan_count"));
		System.out.println("My page likes: " + page.getFanCount());

	}

	public void uploadFB(MultipartFile file) throws IOException {
		long filesizeInBytes = file.getSize();

		FacebookClient fbc = new DefaultFacebookClient(accessToken.getAccessToken(), Version.LATEST);
		ResumableUploadStartResponse returnValue = fbc.publish("Meenayakuda-271725233519430/videos",
				ResumableUploadStartResponse.class, com.restfb.Parameter.with("upload_phase", "start"),
				com.restfb.Parameter.with("file_size", filesizeInBytes));

		long startOffset = returnValue.getStartOffset();
		long endOffset = returnValue.getEndOffset();
		Long length = endOffset - startOffset;

		String uploadSessionId = returnValue.getUploadSessionId();

		while (length > 0) {
			byte fileBytes[] = new byte[length.intValue()];
			file.getInputStream().read(fileBytes);

			ResumableUploadTransferResponse filePart = fbc.publish("Meenayakuda-271725233519430/videos",
					ResumableUploadTransferResponse.class,
					BinaryAttachment.with("video_file_chunk", fileBytes),
					com.restfb.Parameter.with("upload_phase", "transfer"),
					com.restfb.Parameter.with("start_offset", startOffset),
					com.restfb.Parameter.with("upload_session_id", uploadSessionId));

			startOffset = filePart.getStartOffset();
			endOffset = filePart.getEndOffset();
			length = endOffset - startOffset;
		}

		GraphResponse finishResponse = fbc.publish("Meenayakuda-271725233519430/videos", GraphResponse.class,
				com.restfb.Parameter.with("upload_phase", "finish"), // Tell Facebook to finish the upload
				com.restfb.Parameter.with("upload_session_id", uploadSessionId));

		System.out.println("::::: TESTING :::::" + finishResponse.isSuccess());

	}

	public String uploadVideoToYoutube(MultipartFile file) throws IOException {

		// upload video
		String VIDEO_FILE_FORMAT = "video/*";

		Video videoObjectDefiningMetadata = new Video();

		VideoStatus status = new VideoStatus();
		status.setPrivacyStatus("public");
		videoObjectDefiningMetadata.setStatus(status);

		VideoSnippet snippet = new VideoSnippet();

		Calendar cal = Calendar.getInstance();
		snippet.setTitle("Test Upload via Java on " + cal.getTime());
		snippet.setDescription(
				"Video uploaded via YouTube Data API V3 using the Java library " + "on " + cal.getTime());

		List<String> tags = new ArrayList<String>();
		tags.add("test");
		tags.add("example");
		tags.add("java");
		tags.add("YouTube Data API V3");
		tags.add("erase me");
		snippet.setTags(tags);

		videoObjectDefiningMetadata.setSnippet(snippet);

		InputStreamContent mediaContent = new InputStreamContent(VIDEO_FILE_FORMAT, file.getInputStream());

		YouTube.Videos.Insert videoInsert = youtube.videos().insert("snippet,statistics,status",
				videoObjectDefiningMetadata, mediaContent);

		MediaHttpUploader uploader = videoInsert.getMediaHttpUploader();

		uploader.setDirectUploadEnabled(false);

		MediaHttpUploaderProgressListener progressListener = new MediaHttpUploaderProgressListener() {
			public void progressChanged(MediaHttpUploader uploader) throws IOException {
				switch (uploader.getUploadState()) {
				case INITIATION_STARTED:
					System.out.println("Initiation Started");
					break;
				case INITIATION_COMPLETE:
					System.out.println("Initiation Completed");
					break;
				case MEDIA_IN_PROGRESS:
					System.out.println("Upload in progress");
					System.out.println("Upload percentage: " + uploader.getProgress());
					break;
				case MEDIA_COMPLETE:
					System.out.println("Upload Completed!");
					break;
				case NOT_STARTED:
					System.out.println("Upload Not Started!");
					break;
				}
			}
		};
		uploader.setProgressListener(progressListener);

		// Call the API and upload the video.
		Video returnedVideo = videoInsert.execute();

		// Print data about the newly inserted video from the API response.
		System.out.println("\n================== Returned Video ==================\n");
		System.out.println("  - Id: " + returnedVideo.getId());
		System.out.println("  - Title: " + returnedVideo.getSnippet().getTitle());
		System.out.println("  - Tags: " + returnedVideo.getSnippet().getTags());
		System.out.println("  - Privacy Status: " + returnedVideo.getStatus().getPrivacyStatus());
		System.out.println("  - Video Count: " + returnedVideo.getStatistics().getViewCount());
		return returnedVideo.getId();
	}

	private static void prettyPrint(int size, Iterator<PlaylistItem> playlistEntries) {
		System.out.println("=============================================================");
		System.out.println("\t\tTotal Videos Uploaded: " + size);
		System.out.println("=============================================================\n");

		while (playlistEntries.hasNext()) {
			PlaylistItem playlistItem = playlistEntries.next();
			System.out.println(" video name  = " + playlistItem.getSnippet().getTitle());
			System.out.println(" video id    = " + playlistItem.getContentDetails().getVideoId());
			System.out.println(" upload date = " + playlistItem.getSnippet().getPublishedAt());
			System.out.println("\n-------------------------------------------------------------\n");
		}
	}

}
