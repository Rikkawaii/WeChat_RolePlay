package com.example.springaidemo.manage;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.example.springaidemo.model.PixivPictureDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PixivManager {
    public static PixivPictureDTO getPictureByPid(String pid) {
        long st1 = System.currentTimeMillis();
        String refer = "https://www.pixiv.net/";
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36 Edg/135.0.0.0";
        String cookie = "first_visit_datetime_pc=2025-03-04%2017%3A54%3A52; p_ab_id=9; p_ab_id_2=9; p_ab_d_id=1878356998; __utma=235335808.424885314.1741078494.1741078494.1741078494.1; __utmz=235335808.1741078494.1.1.utmcsr=saucenao.com|utmccn=(referral)|utmcmd=referral|utmcct=/; yuid_b=KVkCWUA; c_type=21; privacy_policy_notification=0; a_type=0; b_type=0; __utmv=235335808.|3=plan=normal=1^6=user_id=69445712=1^11=lang=zh=1; PHPSESSID=69445712_qaqqUiHjFguTIgsHF1XWyVRV263k6NK6; device_token=5d39a4ccd5e9745014c21ccc3bf6b4ad; privacy_policy_agreement=0; _ga_MZ1NL4PHH0=GS1.1.1745040243.2.1.1745040257.0.0.0; _gid=GA1.2.1911031534.1745040381; cf_clearance=g63t2KefdVhAJTW76i2fi.pzqg9U_MYeoYk2cNDfs5Y-1745130815-1.2.1.1-XkH6Se3JBrc2DtvwA__TyImbtC2h6yQp3dUtybyMrgc8O4JJ72XyYZmhJm.frd6SxVHZ0JahjDixGtD3a7JMz.aX0A.NGSNYUBorMD3SS6w3kto_5uyBMQzduUrOYQk0Z56b988osoBxcgYdlhRuiIENB.97mUHEUU3LRmemErpvLZoEZaIT2ExL1kIVlWqNDErFroV6bWbPx5npYRYn3pI.yk3FQLTDixxcyZeeRGZKZ2r6HyOajSyBttI_wb2StJYij6sbfo67On81jGwFCj9p4GuaK8bf1OUcTpUearGpcJ3uNZpn6mg2cNgs5qH66UI_xZgi9AtcaDy44ihDw8HCNUhXAvqYdDTVMsO3iMM; _ga=GA1.1.1768821183.1741078496; __cf_bm=XFDxIXNit0EciPpfrezxZ7WqLU8DBtidwnp0KvTHZqU-1745130872-1.0.1.1-zmRHuhy2HyAv7WJk5aZHSD3Rs4RNRPoPvIUoFrEdH13vRUZWu8ZY87pGL_RJja6xNJPRW7BAP84z77j3shAfKcCzXl.sMvUYg.NVehvtzvfIijaivCO3CAmvFTmJJDK1; _ga_75BBYNYN9J=GS1.1.1745130757.4.1.1745130889.0.0.0";
        HttpResponse response = HttpRequest
                .get("https://www.pixiv.net/ajax/illust/" + pid + "?lang=zh")
                .setHttpProxy("127.0.0.1", 7890)  // 代理IP和端口
                .header("referer", refer)
                .header("user-agent", userAgent)
                .header("cookie", cookie)
                .execute();
        String jsonStr = response.body();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = null;
        try {
            root = mapper.readTree(jsonStr);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
        // 提取 original 图片 URL
        String originalUrl = root.path("body")
                .path("urls")
                .path("original")
                .asText();
        // 提取标题
        String title = root.path("body")
                .path("title")
                .asText();
        // 提取作者信息
        String authorId = root.path("body")
                .path("userId")
                .asText();
        String authorName = root.path("body")
                .path("userName")
                .asText();
        // 记录日志
        long ed1 = System.currentTimeMillis();
        log.info("pixiv图片爬取耗时：{}ms", ed1 - st1);
        // 构造 PixivPictureDTO 对象
        PixivPictureDTO pictureDTO = new PixivPictureDTO();
        pictureDTO.setId(pid);
        pictureDTO.setTitle(title);
        pictureDTO.setAuthorId(authorId);
        pictureDTO.setAuthorName(authorName);
        pictureDTO.setUrl(originalUrl);
        return pictureDTO;
    }
}
