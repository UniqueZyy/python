package com.sy.service.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.json.JSONUtil;
import com.sy.pojo.dto.FileDTO;
import com.sy.pojo.dto.ResultDTO;
import com.sy.pojo.dto.UploadDTO;
import com.sy.service.FileService;
import com.sy.tools.AliYunPictureAnalysis;
import com.sy.tools.PictureAnalysis;
import com.sy.utils.PdfToPng;
import com.sy.utils.TextUtil;
import io.goeasy.GoEasy;
import io.goeasy.publish.GoEasyError;
import io.goeasy.publish.PublishListener;
import jakarta.servlet.http.HttpServletRequest;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Auther: Administrator
 * @Description
 * @Date: 2023/11/22
 */
@Service
public class FileServiceImpl implements FileService {

    private static final String FILE_PDF = "/pdf";
    private static final String FILE_PNG = "/png";
    private static final String FILE_JPG = "/jpg";
    private static final String FILE_JPEG = "/jpeg";

    private static final String FILE_NAME_PDF = ".pdf";
    private static final String FILE_NAME_PNG = ".png";
    private static final String FILE_NAME_JPG = ".jpg";
    private static final String FILE_NAME_JPEG = ".jpeg";


    private static final String PYTHON_URL = "http://pyocr.zx-so.com";

    @Value("${file.upload.dir}")
    private String uploadDir;

    private final String RESOURCES_DIR = "D:\\WorkSpace\\work\\sy\\drawing-inspection\\src\\main\\resources\\";

    /**
     * @param file        需要上传的文件
     * @param newFileName 新的文件名
     * @return 返回上传后的文件路径列表
     * @throws IOException
     */
    @Override
    public List<String> upload(MultipartFile file, String newFileName) throws IOException {
        List<String> list = new ArrayList<>();
        // 获取文件名
        String fileName = file.getOriginalFilename();
        // 获取文件类型
        String fileType = fileName.substring(fileName.lastIndexOf("."));
        // 获取文件内容类型
        String contentType = file.getContentType();
        // 获取文件类型
        String type = contentType.substring(contentType.lastIndexOf("/"));
        // 判断文件类型是否符合PDF、JPG、PNG、JPEG
        boolean result = (type.equalsIgnoreCase(FILE_PDF) && fileType.equalsIgnoreCase(FILE_NAME_PDF)) ||
                (type.equalsIgnoreCase(FILE_JPG) && fileType.equalsIgnoreCase(FILE_NAME_JPG)) ||
                (type.equalsIgnoreCase(FILE_JPEG) && fileType.equalsIgnoreCase(FILE_NAME_JPG)) ||
                (type.equalsIgnoreCase(FILE_PNG) && (fileType.equalsIgnoreCase(FILE_NAME_PNG)));
        if (!result) {
            list.add("文件格式不符合");
            return list;
        }
        UUID uuid = UUID.randomUUID();
        // 生成新的文件名
        Path path = Paths.get(uploadDir, uuid + fileType);
        // 获取文件的父路径
        Path parent = path.getParent();
        // 如果不存在则创建
        if (!Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        String string = path.toString();
        System.out.println(string);
        // 找到static的索引
        int indexOf = string.indexOf("\\static\\");
        // 获取输出路径
        String output = string.substring(indexOf + 1);
        // 把反斜杠替换为正斜杠
        output = output.replace("\\", "/");
        try {
            // 判断是否是PDF 如果是
            if (type.equalsIgnoreCase(FILE_PDF) && fileType.equalsIgnoreCase(FILE_NAME_PDF)) {
                // 调用PdfToPng方法
                return PdfToPng(file);
            } else {
                // 写入文件
                Files.write(path, file.getBytes());
            }
            // 添加输出路径到列表
            list.add(output);
            // 返回列表
            return list;
           /*
           try {
                if (type.equalsIgnoreCase(FILE_PDF) && fileType.equalsIgnoreCase(FILE_NAME_PDF)) {
                    PdfToPng pdfToPng = new PdfToPng();
                    path = pdfToPng.pdfToImage(file);
                    String pathString = path.toString();
                    System.out.println("pathString：" + pathString);
                    int index = pathString.indexOf("\\static\\");
                    String substring = pathString.substring(index + 1);
                    substring = substring.replace("\\", "/");
                    return substring;
                } else {
                    Files.write(path, file.getBytes());
                }
                return output;
                */
        } catch (IOException e) {
            list.add("上传失败");
            return list;
        }
    }


    /**
     * @param originalFilename      原始文件名
     * @param uploadList            上传文件列表
     * @param request               HTTP请求对象
     * @param uuid                  标识符
     * @param data                  数据
     * @param sprinkler_max_spacing 洒水喷头最大间距
     * @param diamond_max_spacing   正方形最大间距
     * @return
     */
    @Override
    public List<FileDTO> dispose(String originalFilename, List<String> uploadList, HttpServletRequest request, String uuid, String data, Double sprinkler_max_spacing, Double diamond_max_spacing) {
        AliYunPictureAnalysis aliYunPictureAnalysis = new AliYunPictureAnalysis();
        String baseUrl = getBaseUrl(request);
        // 生成新的文件名
        String name = String.valueOf(UUID.randomUUID());
        List<FileDTO> fileDTOList = new ArrayList<>();
        FileDTO fileDTO1 = new FileDTO();
        int count = 1;
        for (String upload : uploadList) {
            if (firstImg(originalFilename, uploadList, uuid, aliYunPictureAnalysis, baseUrl, name, fileDTOList, fileDTO1))
                return fileDTOList;
            try {
                FileDTO fileDTO = new FileDTO();
                // 如果为空传默认值
                if (Objects.isNull(sprinkler_max_spacing)) {
                    sprinkler_max_spacing = 2.5;
                }
                // 如果为空传默认值
                if (Objects.isNull(diamond_max_spacing)) {
                    diamond_max_spacing = 10.0;
                }
                // 进行文字识别
                UploadDTO uploadDTO = aliYunPictureAnalysis.textOcr(baseUrl + "/" + upload, count);
                fileDTO.setImgPath(upload);//baseUrl + "/" +
                fileDTO.setTitle(originalFilename);
                fileDTO.setUuid(uuid);
                fileDTO.setDocPath(uploadDTO.getDocPath());//baseUrl + "/" +
                // 把fileDTO对象转为json发送给Python进行处理
                String jsonStr = JSONUtil.toJsonStr(fileDTO);
                // 调用Python的upload_room方法进行处理
                URL url = new URL(PYTHON_URL + "/upload_room");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                connection.setConnectTimeout(999999999);
                try (DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
                    out.write(jsonStr.getBytes());
                    out.flush();
                }
                // 处理响应
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while (Objects.nonNull(inputLine = in.readLine())) {
                    response.append(inputLine);
                }
                // 把处理返回来的json数据转为对象
                ResultDTO resultDTO = JSONUtil.toBean(String.valueOf(response), ResultDTO.class);
                fileDTO.setRoom_img_path(resultDTO.getRoom_img_path());
                fileDTO.setData(data);
                fileDTO.setDocPath(resultDTO.getDoc_path());
                fileDTO.setBelow_sprinkler_path(resultDTO.getBelow_sprinkler_path());
                fileDTO.setSprinkler_max_spacing(sprinkler_max_spacing);
                System.out.println("fileDTO upload_fire_sprinkler：" + fileDTO);
                // 再把对象转为json发送给Python进行处理
                String jsonStr1 = JSONUtil.toJsonStr(fileDTO);
                System.out.println("jsonStr1upload_fire_sprinkler" + jsonStr1);
                // 调用Python的upload_fire_sprinkler方法进行处理
                URL url1 = new URL(PYTHON_URL + "/upload_fire_sprinkler");
                HttpURLConnection connection1 = (HttpURLConnection) url1.openConnection();
                connection1.setRequestMethod("POST");
                connection1.setRequestProperty("Content-Type", "application/json");
                connection1.setDoOutput(true);
                connection1.setConnectTimeout(999999999);
                try (DataOutputStream out1 = new DataOutputStream(connection1.getOutputStream())) {
                    out1.write(jsonStr1.getBytes());
                    out1.flush();
                }
                // 处理响应
                BufferedReader in1 = new BufferedReader(new InputStreamReader(connection1.getInputStream()));
                String inputLine1;
                StringBuilder response1 = new StringBuilder();
                while (Objects.nonNull(inputLine1 = in1.readLine())) {
                    response1.append(inputLine1);
                }
                // 把处理完的json数据转为对象
                ResultDTO resultDTO1 = JSONUtil.toBean(String.valueOf(response1), ResultDTO.class);
                fileDTO.setPass_sprinkler_path(resultDTO1.getBelow_sprinkler_path());
                fileDTO.setActual_distance(resultDTO1.getActual_distance());
                fileDTO.setBelow_sprinkler_path(resultDTO1.getBelow_sprinkler_path());
                fileDTO.setDiamond_max_spacing(diamond_max_spacing);
                System.out.println("diamond_max_spacing:" + diamond_max_spacing);
                // 再把对象转为json发送给Python进行处理
                String jsonStr2 = JSONUtil.toJsonStr(fileDTO);
                System.out.println("jsonStr2:" + jsonStr2);
                // 调用Python的upload_rectangle方法进行处理
                URL url2 = new URL(PYTHON_URL + "/upload_rectangle");
                HttpURLConnection connection2 = (HttpURLConnection) url2.openConnection();
                connection2.setRequestMethod("POST");
                connection2.setRequestProperty("Content-Type", "application/json");
                connection2.setDoOutput(true);
                connection2.setConnectTimeout(999999999);
                try (DataOutputStream out2 = new DataOutputStream(connection2.getOutputStream())) {
                    out2.write(jsonStr2.getBytes());
                    out2.flush();
                }
                // 处理响应
                BufferedReader in2 = new BufferedReader(new InputStreamReader(connection2.getInputStream()));
                String inputLine2;
                StringBuilder response2 = new StringBuilder();
                while (Objects.nonNull(inputLine2 = in2.readLine())) {
                    response2.append(inputLine2);
                }
                ResultDTO resultDTO2 = JSONUtil.toBean(String.valueOf(response2), ResultDTO.class);
                fileDTO.setResult_img_path(resultDTO2.getResult_img_path());
                fileDTO.setRectangle_avg(resultDTO2.getRectangle_avg());
                // 返回处理结果添加到一个list里
                fileDTOList.add(fileDTO);
                in.close();
                in1.close();
                in2.close();
                count++;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return fileDTOList;
    }

    private final static String JAVA_URL = "http://javaocr.zx-so.com/";

    /**
     * @param originalFilename      原始文件名
     * @param uploadList            上传文件列表
     * @param request               HTTP请求对象
     * @param uuid                  标识符
     * @param data                  数据
     * @param sprinkler_max_spacing 洒水喷头最大间距
     * @param diamond_max_spacing   正方形最大间距
     * @return
     */
    @Override
    public List<FileDTO> disposeV2(String originalFilename, List<String> uploadList, HttpServletRequest request, String uuid, String data, Double sprinkler_max_spacing, Double diamond_max_spacing) {
        Map<String, String> map = new HashMap<>();
        List<FileDTO> fileDTOList = new ArrayList<>();
        String decodedResponse = null;
        String pdfFilePath = "";
        boolean isFirst = false;
        // 如果为空传默认值
        sprinkler_max_spacing = Objects.requireNonNullElse(sprinkler_max_spacing, 2.5);
        // 如果为空传默认值
        diamond_max_spacing = Objects.requireNonNullElse(diamond_max_spacing, 10.0);
        String result = null;
        for (String upload : uploadList) {
            try {
//                String response = urlConnection("OCR_Service", upload, "text");
                // 创建 URL 对象
                URL obj = new URL(PYTHON_URL + "/OCR_Service");

                // 打开连接
                HttpURLConnection connection = (HttpURLConnection) obj.openConnection();

                // 设置请求方法为 POST
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(0);
                connection.setReadTimeout(0);
                // 启用输出流
                connection.setDoOutput(true);
                // 设置请求头部
                connection.setRequestProperty("Content-Type", "text/plain");

                // 读取文件内容并写入请求体
//                DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
//                wr.writeBytes(upload);
//                wr.flush();
//                wr.close();
                try (DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
                    out.write(upload.getBytes());
                    out.flush();
                }
                // 读取响应内容
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                byte[] bytes = String.valueOf(response).getBytes(StandardCharsets.UTF_8);
                decodedResponse = new String(bytes, StandardCharsets.UTF_8);
                System.out.println("decodedResponse：" + decodedResponse);
                map.put(upload, decodedResponse);
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("map：" + map);
        Collection<String> values = map.values();
        String allValue = String.valueOf(values);
        System.out.println("allValue：" + allValue);
        FileDTO fileDTO = new FileDTO();
        if (!allValue.contains("图纸目录") && !allValue.contains("图纸自录")) {
            System.out.println("该消防图纸没有目录");
            result = "该消防图纸没有目录";
            fileDTO.setUuid(uuid);
            fileDTO.setResult(result);
            fileDTO.setTitle(originalFilename);
            isFirst = true;
            fileDTO.setIsFirst(isFirst);
            fileDTOList.add(fileDTO);
            String test = test(fileDTO);
            System.out.println("test:" + test);
            return fileDTOList;
        }
        int count = 1;
        String docName = String.valueOf(UUID.randomUUID());
        System.out.println("docName：" + docName);
        for (String upload : uploadList) {
            String value = map.get(upload);
            System.out.println("该消防图纸有目录");
            System.out.println(upload + "：value:" + value);
            String imgUrl = JAVA_URL + upload;
            if (value.contains("设计说明")) {
                TextUtil.contains(decodedResponse, ".*消防.*");
                //pdfFilePath = pictureAnalysis.textToPDF("该图片是消防图片");
                // 判断是否含有建筑设计防火规范2014和2018 或 判断是否含有防火等级一级和大于10000的数字    判断是否含有防火等级而级和大于20000的数字
                if ((TextUtil.contains(decodedResponse, ".*建筑设计防火规范2014.*") && TextUtil.contains(decodedResponse, ".*2018.*"))
                        || (TextUtil.containsStructureAreaGt10000(decodedResponse) && TextUtil.contains(decodedResponse, ".*防火等级一级*"))
                        || (TextUtil.containsStructureAreaGt20000(decodedResponse) && TextUtil.contains(decodedResponse, ".*防火等级二级*"))) {
                    //textToWord("合格");
                    System.out.println("目录" + count + "合格" + "图片地址：");
                    result = "目录" + count + "合格" + "图片地址：";
                } else {
                    if ((!(TextUtil.containsStructureAreaGt10000(decodedResponse) && TextUtil.contains(decodedResponse, ".*防火等级一级*")))) {
                        //textToWord("不包含防火等级一级和大于10000的数字  不合格");
                        result = "目录" + count + "不包含防火等级一级和大于10000的数字  不合格" + "图片地址：";
                        System.out.println("目录" + count + "不包含防火等级一级和大于10000的数字  不合格" + "图片地址：");
                    } else if (!(TextUtil.contains(decodedResponse, ".*建筑设计防火规范2014.*") && TextUtil.contains(decodedResponse, ".*2018.*"))) {
                        //textToWord("不包含建筑设计防火规范2014和2018  不合格");
                        result = "目录" + count + "不包含建筑设计防火规范2014和2018  不合格" + "图片地址：";
                        System.out.println("目录" + count + "不包含建筑设计防火规范2014和2018  不合格" + "图片地址：");
                    } else if (!(TextUtil.containsStructureAreaGt20000(decodedResponse) && TextUtil.contains(decodedResponse, ".*防火等级二级*"))) {
                        //textToWord("不包含防火等级二级和大于20000的数字  不合格");
                        result = "目录" + count + "不包含防火等级二级和大于20000的数字  不合格" + "图片地址：";
                        System.out.println("目录" + count + "不包含防火等级二级和大于20000的数字  不合格" + "图片地址：");
                    }
                }
            } else {
                result = "目录" + count + "该图片不含消防二字，不合格" + "图片地址：";
                System.out.println("目录" + count + "该图片不含消防二字，不合格" + "图片地址：");
            }
            count++;
            // 将结果写入 Word 文档
            TextUtil.textToWord(result, imgUrl);
            pdfFilePath = TextUtil.saveDocument(docName);
            fileDTO.setDocPath(pdfFilePath);
            int startIndex = pdfFilePath.lastIndexOf("/") + 1;
            String fileName = pdfFilePath.substring(startIndex);
            System.out.println("fileName：" + fileName);
            fileDTO.setIsFirst(isFirst);
            fileDTO.setTitle(originalFilename);
            fileDTO.setUuid(uuid);
            if (value.contains("平面图") && !value.contains("图纸目录") && !value.contains("设计说明")) {
                System.out.println("处理房间或承重柱upload" + upload);
                // 处理房间
                FileDTO fileDTO1 = new FileDTO();
                fileDTO1.setSprinkler_max_spacing(sprinkler_max_spacing);
                fileDTO1.setImgPath(upload);
                fileDTO1.setUuid(uuid);
                fileDTO1.setDocPath(pdfFilePath);
                fileDTO1.setTitle(originalFilename);
                String jsonStr = JSONUtil.toJsonStr(fileDTO1);
                System.out.println("Json房间：" + jsonStr);
//                String response = urlConnection("upload_room", jsonStr, "json");
                StringBuilder response = null;
                try {
                    URL url = new URL(PYTHON_URL + "/upload_room");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setDoOutput(true);
                    connection.setConnectTimeout(999999999);
                    try (DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
                        out.write(jsonStr.getBytes());
                        out.flush();
                    }
                    // 处理响应
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    response = new StringBuilder();
                    while (Objects.nonNull(inputLine = in.readLine())) {
                        response.append(inputLine);
                    }
                    ResultDTO resultDTO = JSONUtil.toBean(String.valueOf(response), ResultDTO.class);
                    fileDTO1.setRoom_img_path(resultDTO.getRoom_img_path());
                    fileDTO1.setData(data);
                    fileDTO1.setBelow_sprinkler_path(resultDTO.getBelow_sprinkler_path());
                    fileDTO1.setSprinkler_max_spacing(sprinkler_max_spacing);
                    fileDTOList.add(fileDTO1);

                    // 处理承重柱
                    FileDTO fileDTO2 = new FileDTO();
                    fileDTO2.setImgPath(upload);
                    fileDTO2.setUuid(uuid);
                    fileDTO2.setDocPath(pdfFilePath);
                    fileDTO2.setTitle(originalFilename);
                    fileDTO2.setIsFirst(isFirst);
                    fileDTO2.setDiamond_max_spacing(diamond_max_spacing);
                    String jsonStr1 = JSONUtil.toJsonStr(fileDTO2);
                    System.out.println("jsonStr承重柱:" + jsonStr1);
                    URL url1 = new URL(PYTHON_URL + "/upload_rectangle");
                    HttpURLConnection connection1 = (HttpURLConnection) url1.openConnection();
                    connection1.setRequestMethod("POST");
                    connection1.setRequestProperty("Content-Type", "application/json");
                    connection1.setDoOutput(true);
                    connection1.setConnectTimeout(999999999);
                    try (DataOutputStream out = new DataOutputStream(connection1.getOutputStream())) {
                        out.write(jsonStr1.getBytes());
                        out.flush();
                    }
                    // 处理响应
                    BufferedReader in1 = new BufferedReader(new InputStreamReader(connection1.getInputStream()));
                    String inputLine1;
                    StringBuilder response1 = new StringBuilder();
                    while (Objects.nonNull(inputLine1 = in1.readLine())) {
                        response1.append(inputLine1);
                    }
                    // 把处理返回来的json数据转为对象
                    System.out.println("response1：" + response1);
                    ResultDTO resultDTO1 = JSONUtil.toBean(String.valueOf(response1), ResultDTO.class);
                    fileDTO2.setRectangle_avg(resultDTO1.getRectangle_avg());
                    fileDTO2.setResult_img_path(resultDTO1.getResult_img_path());
                    fileDTOList.add(fileDTO2);
                    in.close();
                    in1.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            if (value.contains("喷淋") && !value.contains("图纸目录") && !value.contains("设计说明")) {
                try {
                    System.out.println("处理喷淋头upload" + upload);
                    // 处理喷淋头
                    FileDTO fileDTO1 = new FileDTO();
                    fileDTO1.setSprinkler_max_spacing(sprinkler_max_spacing);
                    fileDTO1.setImgPath(upload);
                    fileDTO1.setData(data);
                    fileDTO1.setUuid(uuid);
                    fileDTO1.setTitle(originalFilename);
                    fileDTO1.setDocPath(pdfFilePath);
                    String jsonStr = JSONUtil.toJsonStr(fileDTO1);
//                    String response = urlConnection("upload_fire_sprinkler", jsonStr, "json");
                    System.out.println("jsonStr喷淋头" + jsonStr);
                    URL url = new URL(PYTHON_URL + "/upload_fire_sprinkler");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setDoOutput(true);
                    connection.setConnectTimeout(999999999);
                    try (DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
                        out.write(jsonStr.getBytes());
                        out.flush();
                    }
                    // 处理响应
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    while (Objects.nonNull(inputLine = in.readLine())) {
                        response.append(inputLine);
                    }
                    // 把处理返回来的json数据转为对象
                    ResultDTO resultDTO = JSONUtil.toBean(String.valueOf(response), ResultDTO.class);
                    fileDTO1.setPass_sprinkler_path(resultDTO.getBelow_sprinkler_path());
                    fileDTO1.setActual_distance(resultDTO.getActual_distance());
                    fileDTO1.setBelow_sprinkler_path(resultDTO.getBelow_sprinkler_path());
                    fileDTO1.setDiamond_max_spacing(diamond_max_spacing);
                    fileDTO1.setNonconformity(resultDTO.getNonconformity());
//                    TextUtil.textToWord(resultDTO.getNonconformity());
                    TextUtil.insertToWord(resultDTO.getNonconformity(), fileName);
                    fileDTOList.add(fileDTO1);
                    in.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
        }
        return fileDTOList;
    }

    /**
     * @param originalFilename      原始文件名
     * @param uploadList            上传文件列表
     * @param request               HTTP请求对象
     * @param uuid                  标识符
     * @param data                  数据
     * @param sprinkler_max_spacing 洒水喷头最大间距
     * @param diamond_max_spacing   正方形最大间距
     * @return
     */

    @Override
    public List<FileDTO> room(String originalFilename, List<String> uploadList, HttpServletRequest request, String uuid, String data, Double sprinkler_max_spacing, Double diamond_max_spacing) {
        AliYunPictureAnalysis aliYunPictureAnalysis = new AliYunPictureAnalysis();
        String baseUrl = getBaseUrl(request);
        // 生成新的文件名
        String name = String.valueOf(UUID.randomUUID());
        FileDTO fileDTO1 = new FileDTO();
        List<FileDTO> fileDTOList = new ArrayList<>();
        // 如果为空传默认值
        sprinkler_max_spacing = Objects.requireNonNullElse(sprinkler_max_spacing, 2.5);
        // 如果为空传默认值
        diamond_max_spacing = Objects.requireNonNullElse(diamond_max_spacing, 10.0);
        int count = 1;
        for (String upload : uploadList) {
            if (firstImg(originalFilename, uploadList, uuid, aliYunPictureAnalysis, baseUrl, name, fileDTOList, fileDTO1))
                return fileDTOList;
            try {
                FileDTO fileDTO = new FileDTO();
                fileDTO.setResult("img");
                // 进行文字识别
                UploadDTO uploadDTO = aliYunPictureAnalysis.textOcr(baseUrl + "/" + upload, count);
                fileDTO.setImgPath(upload);//baseUrl + "/" +
                fileDTO.setTitle(originalFilename);
                fileDTO.setUuid(uuid);
                fileDTO.setDocPath(uploadDTO.getDocPath());//baseUrl + "/" +
                // 把fileDTO对象转为json发送给Python进行处理
                String jsonStr = JSONUtil.toJsonStr(fileDTO);
                // 调用Python的upload_room方法进行处理
                URL url = new URL(PYTHON_URL + "/upload_room");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                connection.setConnectTimeout(999999999);
                try (DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
                    out.write(jsonStr.getBytes());
                    out.flush();
                }
                // 处理响应
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while (Objects.nonNull(inputLine = in.readLine())) {
                    response.append(inputLine);
                }
                // 把处理返回来的json数据转为对象
                ResultDTO resultDTO = JSONUtil.toBean(String.valueOf(response), ResultDTO.class);
                fileDTO.setResult_img_path(resultDTO.getResult_img_path());
                fileDTO.setData(data);
                fileDTO.setBelow_sprinkler_path(resultDTO.getBelow_sprinkler_path());
                fileDTO.setSprinkler_max_spacing(sprinkler_max_spacing);
                fileDTOList.add(fileDTO);
                in.close();
                count++;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
        return fileDTOList;
    }

    /**
     * 处理第一张图片
     *
     * @param originalFilename
     * @param uploadList
     * @param uuid
     * @param aliYunPictureAnalysis
     * @param baseUrl
     * @param name
     * @param fileDTOList
     * @param fileDTO
     * @return
     */
    private boolean firstImg(String originalFilename, List<String> uploadList, String uuid, AliYunPictureAnalysis aliYunPictureAnalysis, String baseUrl, String name, List<FileDTO> fileDTOList, FileDTO fileDTO) {
        // 获取列表中的第一个元素
        String firstUpload = uploadList.get(0);
        // 进行文字识别
        UploadDTO uploadDTO = aliYunPictureAnalysis.textFirstOCR(baseUrl + "/" + firstUpload, name);
        // 把识别结果传输给fileDTO
        fileDTO.setImgPath(firstUpload);
        fileDTO.setIsFirst(!uploadDTO.getIsFirst());
        fileDTO.setResult(uploadDTO.getResult());
        fileDTO.setTitle(originalFilename);
        fileDTO.setDocPath(uploadDTO.getDocPath());
        fileDTO.setUuid(uuid);
        if (fileDTO.getIsFirst()) {
            // 发送消息
            this.below(uuid, fileDTO);
            // 将识别结果添加到list集合
            fileDTOList.add(fileDTO);
            return true;
        }
        return false;
    }

    @Override
    public List<FileDTO> rectangle(String originalFilename, List<String> uploadList, HttpServletRequest request, String uuid, String data, Double sprinkler_max_spacing, Double diamond_max_spacing) {
        AliYunPictureAnalysis aliYunPictureAnalysis = new AliYunPictureAnalysis();
        String baseUrl = getBaseUrl(request);
        // 生成新的文件名
        String name = String.valueOf(UUID.randomUUID());
        List<FileDTO> fileDTOList = new ArrayList<>();
        FileDTO fileDTO1 = new FileDTO();
        // 如果为空传默认值
        sprinkler_max_spacing = Objects.requireNonNullElse(sprinkler_max_spacing, 2.5);
        // 如果为空传默认值
        diamond_max_spacing = Objects.requireNonNullElse(diamond_max_spacing, 10.0);
        int count = 1;
        for (String upload : uploadList) {
            if (firstImg(originalFilename, uploadList, uuid, aliYunPictureAnalysis, baseUrl, name, fileDTOList, fileDTO1))
                return fileDTOList;
            try {
                FileDTO fileDTO = new FileDTO();
                // 进行文字识别
                UploadDTO uploadDTO = aliYunPictureAnalysis.textOcr(baseUrl + "/" + upload, count);
                fileDTO.setImgPath(upload);//baseUrl + "/" +
                fileDTO.setTitle(originalFilename);
                fileDTO.setUuid(uuid);
                fileDTO.setDocPath(uploadDTO.getDocPath());//baseUrl + "/" +
                fileDTO.setDiamond_max_spacing(diamond_max_spacing);
                // 把fileDTO对象转为json发送给Python进行处理
                String jsonStr = JSONUtil.toJsonStr(fileDTO);
                // 调用Python的upload_room方法进行处理
                URL url = new URL(PYTHON_URL + "/upload_rectangle");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                connection.setConnectTimeout(999999999);
                try (DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
                    out.write(jsonStr.getBytes());
                    out.flush();
                }
                // 处理响应
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while (Objects.nonNull(inputLine = in.readLine())) {
                    response.append(inputLine);
                }
                // 把处理返回来的json数据转为对象
                ResultDTO resultDTO = JSONUtil.toBean(String.valueOf(response), ResultDTO.class);
                fileDTO.setResult_img_path(resultDTO.getResult_img_path());
                fileDTO.setRectangle_avg(resultDTO.getRectangle_avg());
                fileDTOList.add(fileDTO);
                in.close();
                count++;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
        return fileDTOList;
    }

    @Override
    public List<FileDTO> fireSprinkler(String originalFilename, List<String> uploadList, HttpServletRequest request, String uuid, String data, Double sprinkler_max_spacing, Double diamond_max_spacing) {
        AliYunPictureAnalysis aliYunPictureAnalysis = new AliYunPictureAnalysis();
        String baseUrl = getBaseUrl(request);
        // 生成新的文件名
        String name = String.valueOf(UUID.randomUUID());
        List<FileDTO> fileDTOList = new ArrayList<>();
        FileDTO fileDTO1 = new FileDTO();
        // 如果为空传默认值
        sprinkler_max_spacing = Objects.requireNonNullElse(sprinkler_max_spacing, 2.5);
        // 如果为空传默认值
        diamond_max_spacing = Objects.requireNonNullElse(diamond_max_spacing, 10.0);
        int count = 1;
        for (String upload : uploadList) {
            if (firstImg(originalFilename, uploadList, uuid, aliYunPictureAnalysis, baseUrl, name, fileDTOList, fileDTO1))
                return fileDTOList;
            try {
                FileDTO fileDTO = new FileDTO();
                // 进行文字识别
                UploadDTO uploadDTO = aliYunPictureAnalysis.textOcr(baseUrl + "/" + upload, count);
                fileDTO.setImgPath(upload);//baseUrl + "/" +
                fileDTO.setTitle(originalFilename);
                fileDTO.setUuid(uuid);
                fileDTO.setDocPath(uploadDTO.getDocPath());//baseUrl + "/" +
                fileDTO.setData(data);
                fileDTO.setSprinkler_max_spacing(sprinkler_max_spacing);
                // 把fileDTO对象转为json发送给Python进行处理
                String jsonStr = JSONUtil.toJsonStr(fileDTO);
                // 调用Python的upload_room方法进行处理
                URL url = new URL(PYTHON_URL + "/upload_fire_sprinkler");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                connection.setConnectTimeout(999999999);
                try (DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
                    out.write(jsonStr.getBytes());
                    out.flush();
                }
                // 处理响应
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while (Objects.nonNull(inputLine = in.readLine())) {
                    response.append(inputLine);
                }
                // 把处理返回来的json数据转为对象
                ResultDTO resultDTO = JSONUtil.toBean(String.valueOf(response), ResultDTO.class);
                System.out.println("resultDTO：" + resultDTO);
                fileDTO.setPass_sprinkler_path(resultDTO.getBelow_sprinkler_path());
                fileDTO.setActual_distance(resultDTO.getActual_distance());
                fileDTO.setResult_img_path(resultDTO.getResult_img_path());
                fileDTO.setDiamond_max_spacing(diamond_max_spacing);
                fileDTO.setNonconformity(resultDTO.getNonconformity());
                System.out.println("fileDTO fire_sprinkler" + fileDTO);
                aliYunPictureAnalysis.textToWord(resultDTO.getNonconformity());
                fileDTOList.add(fileDTO);
                in.close();
                count++;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
        return fileDTOList;
    }

    @Override
    public String test(FileDTO fileDTO) {
        System.out.println("c：" + fileDTO);
        String jsonStr = JSONUtil.toJsonStr(fileDTO);
        System.out.println("jsonStr11111111111：" + jsonStr);
        GoEasy goEasy = new GoEasy("https://rest-hz.goeasy.io", "PR-f80d5629ea0e40cd8e71df1de7653f45");
        goEasy.publish(fileDTO.getUuid(), jsonStr, new PublishListener() {
            @Override
            public void onSuccess() {
                System.out.println("消息发送成功");
            }

            @Override
            public void onFailed(GoEasyError error) {
                System.out.println("消息发送失败 错误为：" + error.getCode() + " , " + error.getContent());
            }
        });
        return jsonStr;
    }

    public void below(String uuid, FileDTO fileDTO) {
        String jsonStr = JSONUtil.toJsonStr(fileDTO);
        GoEasy goEasy = new GoEasy("https://rest-hz.goeasy.io", "PR-f80d5629ea0e40cd8e71df1de7653f45");
        goEasy.publish(uuid, jsonStr, new PublishListener() {
            @Override
            public void onSuccess() {
                System.out.println("消息发送成功");
            }

            @Override
            public void onFailed(GoEasyError error) {
                System.out.println("消息发送失败 错误为：" + error.getCode() + " , " + error.getContent());
            }
        });
    }

    @NotNull
    private static List<String> PdfToPng(MultipartFile file) throws IOException {
        PdfToPng pdfToPng = new PdfToPng();
        List<Path> pathList = pdfToPng.pdfToImage(file);
        List<String> substringList = new ArrayList<>();
        for (Path path1 : pathList) {
            String pathString = path1.toString();
            int index = pathString.indexOf("\\static\\");
            String substring = pathString.substring(index + 1);
            substring = substring.replace("\\", "/");
            // 添加子字符串到列表
            substringList.add(substring);
        }
        // 返回列表
        return substringList;
    }

    private static String getBaseUrl(HttpServletRequest request) {
        String baseUrl = request.getRequestURL().toString();
        String requestURI = request.getRequestURI();
        // 获取请求网址
        baseUrl = baseUrl.substring(0, baseUrl.indexOf(requestURI));
        return baseUrl;
    }

    private static String urlConnection(String method, String file, String type) {
        StringBuilder response = new StringBuilder();
        try {
            // 定义 Flask API 的 URL
            String url = PYTHON_URL + "/" + method;

            // 创建 URL 对象
            URL obj = new URL(url);

            // 打开连接
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();

            // 设置请求方法为 POST
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(999999999);
            // 启用输出流
            connection.setDoOutput(true);
            if (Objects.equals(type, "text")) {
                type = "text/plain";
            }
            if (Objects.equals(type, "json")) {
                type = "application/json";
            }
            // 设置请求头部
            connection.setRequestProperty("Content-Type", type);

            // 读取文件内容并写入请求体
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(file);
            wr.flush();
            wr.close();

            // 读取响应内容
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return String.valueOf(response);
    }
}
