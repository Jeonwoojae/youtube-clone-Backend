package com.semtleWebGroup.youtubeclone.domain.video.service;

import com.semtleWebGroup.youtubeclone.domain.channel.domain.Channel;
import com.semtleWebGroup.youtubeclone.domain.channel.repository.ChannelRepository;
import com.semtleWebGroup.youtubeclone.domain.video.domain.Video;
import com.semtleWebGroup.youtubeclone.domain.video.dto.*;
import com.semtleWebGroup.youtubeclone.domain.video.repository.VideoRepository;
import com.semtleWebGroup.youtubeclone.domain.video_media.service.MediaServerSpokesman;
import com.semtleWebGroup.youtubeclone.global.error.exception.EntityNotFoundException;
import com.semtleWebGroup.youtubeclone.global.error.exception.MediaServerException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Blob;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VideoService {
    private final VideoRepository videoRepository;
    private final MediaServerSpokesman mediaServerSpokesman;

    public Video getVideo(UUID videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(()-> new EntityNotFoundException("Video is not found."));
        return video;
    }

    public VideoResponse upload(VideoUploadDto dto) throws MediaServerException {
        Video video = Video.builder()
                .channel(dto.getChannel())
                .build();
        videoRepository.save(video);

        mediaServerSpokesman.sendEncodingRequest(dto.getVideoFile(), video.getVideoId(), dto.getThumbImg());
        return new VideoResponse(video);
    }

    @Transactional
    public VideoViewResponse view(UUID videoId, Channel channel) {
        Video video = this.getVideo(videoId);
        video.incrementViewCount();
        videoRepository.save(video);

        VideoViewResponse videoViewResponse = VideoViewResponse.builder()
            .video(video)
            .isLike(video.isLike(channel))
//                .qualityList(mediaServerSpokesman.getQualityList(video.getVideoId())) // TODO
            .build();
        return videoViewResponse;
    }

    @Transactional
    public VideoResponse edit(VideoEditDto dto) {
        Video video = this.getVideo(dto.getVideoId());
        // TODO: 권한 확인
        if (dto.getThumbImg() == null)
            video.update(dto.getTitle(), dto.getDescription());
        else
            video.update(dto.getTitle(), dto.getDescription(), dto.getThumbImg());
        videoRepository.save(video);
        return new VideoResponse(video);
    }

    @Transactional
    public VideoResponse delete(UUID videoId, Channel channel) throws MediaServerException {
        // TODO: 권한 확인
        mediaServerSpokesman.deleteVideo(videoId);
        Video video = this.getVideo(videoId);
        videoRepository.delete(video);
        return new VideoResponse(video);
    }
}
