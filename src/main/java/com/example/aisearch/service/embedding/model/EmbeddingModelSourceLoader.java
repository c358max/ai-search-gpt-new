package com.example.aisearch.service.embedding.model;

import com.example.aisearch.config.AiSearchProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class EmbeddingModelSourceLoader {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingModelSourceLoader.class);
    private final ResourceLoader resourceLoader;
    private final AiSearchProperties properties;

    public EmbeddingModelSourceLoader(ResourceLoader resourceLoader, AiSearchProperties properties) {
        this.resourceLoader = resourceLoader;
        this.properties = properties;
    }

    /**
     * 임베딩 모델 소스를 결정한다.
     *
     * <p>우선순위:
     * <ol>
     *   <li>로컬 경로(embedding-model-path)가 유효하면 로컬 모델 사용</li>
     *   <li>그렇지 않으면 URL(embedding-model-url) 사용</li>
     * </ol>
     *
     * @return 로컬 경로 또는 URL을 담은 {@link EmbeddingModelSource}
     */
    public EmbeddingModelSource load() throws IOException {
        // 로컬 경로에 컴파일된 모델이 있으면 우선 이용한다.
        String modelPath = properties.embeddingModelPath();
        if (modelPath != null && !modelPath.isBlank() && !"__NONE__".equalsIgnoreCase(modelPath.trim())) {
            Path resolvedPath = resolveModelPath(modelPath.trim());
            log.info("[EMBED_MODEL] using model path: {} -> {}", modelPath, resolvedPath);
            
            return new EmbeddingModelSource(resolvedPath, null, true);
        }

        String modelUrl = properties.embeddingModelUrl();
        log.info("[EMBED_MODEL] using model url: {}", modelUrl);
        return new EmbeddingModelSource(null, modelUrl, false);
    }

    private Path resolveModelPath(String modelPath) throws IOException {
        if (modelPath.startsWith("/")) {
            Path path = Paths.get(modelPath);
            if (!Files.exists(path)) {
                throw new IOException("Model path does not exist: " + modelPath);
            }
            return path;
        }

        if (modelPath.startsWith("file:")) {
            Resource resource = resourceLoader.getResource(modelPath);
            return resource.getFile().toPath();
        }

        Resource resource = resourceLoader.getResource(modelPath);
        return resource.getFile().toPath();
    }
}
