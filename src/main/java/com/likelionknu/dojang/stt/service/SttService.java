package com.likelionknu.dojang.stt.service;

import com.likelionknu.dojang.common.response.ErrorCode;
import com.likelionknu.dojang.stt.data.dto.response.SttResponseDto;
import com.likelionknu.dojang.stt.exception.SttException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SttService {
    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final Set<String> WEBM_MIMES = Set.of("audio/webm", "video/webm");
    private static final Set<String> WAV_MIMES = Set.of("audio/wav", "audio/x-wav", "audio/wave", "audio/vnd.wave", "audio/vnd.wav");
    private final SttClient sttClient;

    public SttResponseDto transcribe(MultipartFile audio) {
        if (audio == null || audio.isEmpty()) throw invalid("audio: 음성 파일이 필요합니다.");
        if (audio.getSize() > MAX_BYTES) throw invalid("audio: 최대 10MiB까지 가능합니다.");
        byte[] bytes;
        try { bytes = audio.getBytes(); }
        catch (IOException exception) { throw invalid("audio: 파일을 읽을 수 없습니다."); }
        if (bytes.length == 0 || bytes.length > MAX_BYTES) throw invalid("audio: 파일 크기가 허용 범위가 아닙니다.");
        String format = format(bytes);
        validateMime(audio.getContentType(), format);
        validateFilename(audio.getOriginalFilename(), format);
        try {
            return new SttResponseDto(sttClient.transcribe(bytes, "recording." + format));
        } catch (RuntimeException exception) {
            throw new SttException(ErrorCode.AI_FAILED, "음성 변환에 실패했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    private String format(byte[] bytes) {
        if (bytes.length >= 4 && (bytes[0] & 0xff) == 0x1a && (bytes[1] & 0xff) == 0x45
                && (bytes[2] & 0xff) == 0xdf && (bytes[3] & 0xff) == 0xa3) return "webm";
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'A' && bytes[10] == 'V' && bytes[11] == 'E') return "wav";
        throw invalid("audio: WebM 또는 WAV 파일만 가능합니다.");
    }

    private void validateMime(String contentType, String format) {
        if (contentType == null || contentType.isBlank()) return;
        String mime;
        try {
            MediaType parsed = MediaType.parseMediaType(contentType);
            mime = (parsed.getType() + "/" + parsed.getSubtype()).toLowerCase(Locale.ROOT);
        } catch (InvalidMediaTypeException exception) {
            throw invalid("audio: 올바른 Content-Type이 아닙니다.");
        }
        if (mime.equals("application/octet-stream")) return;
        if (format.equals("webm") ? WEBM_MIMES.contains(mime) : WAV_MIMES.contains(mime)) return;
        throw invalid("audio: 파일 형식과 Content-Type이 일치하지 않습니다.");
    }

    private void validateFilename(String filename, String format) {
        if (filename == null || filename.isBlank()) return;
        int dot = filename.lastIndexOf('.');
        if (dot < 0) return; // Browser recordings may use the extensionless name "blob".
        String extension = filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!extension.equals(format)) throw invalid("audio: 파일 확장자와 형식이 일치하지 않습니다.");
    }

    private static SttException invalid(String message) { return new SttException(ErrorCode.BAD_REQUEST, message); }
}
