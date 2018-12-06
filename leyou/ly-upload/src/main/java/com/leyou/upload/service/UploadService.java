package com.leyou.upload.service;

import com.github.tobato.fastdfs.domain.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exceptions.LyException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class UploadService {

    private static final List<String> ALLOW_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/png", "image/bmp");

    @Autowired
    private FastFileStorageClient storageClient;

    public String uploadImage(MultipartFile file) {
        try {
            // 校验文件
            // 1，校验文件类型
            String contentType = file.getContentType();
            if (!ALLOW_CONTENT_TYPES.contains(contentType)) {
                throw new LyException(ExceptionEnum.INVALID_FILE_TYPE);
            }

            // 2, 校验文件内容
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new LyException(ExceptionEnum.INVALID_FILE_TYPE);
            }

            // 保存到本地
//            File dest = new File("C:\\lesson\\heima46\\nginx-1.12.2\\html", file.getOriginalFilename());
//            file.transferTo(dest);

            // 获取后缀名
            String extension = StringUtils.substringAfterLast(file.getOriginalFilename(), ".");

            // 3 上传到FastDFS
            StorePath storePath = storageClient.uploadFile(file.getInputStream(), file.getSize(), extension, null);

            // 返回地址
            String url = "http://image.leyou.com/" + storePath.getFullPath();
            return url;
        }catch (Exception e){
            log.error("【文件上传服务】文件上传失败！", e);
            throw new LyException(ExceptionEnum.FILE_UPLOAD_ERROR);
        }
    }
}
