import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @description:
 * @author: Rinhon
 * @date 2021年08月24日 16:28
 */
public class ImagesUtil {

    /**
     * 获取CPU个数
     */
    private static final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    /**
     * 创建线程池  调整队列数 拒绝服务
     */
    private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(CORE_POOL_SIZE, CORE_POOL_SIZE+1, 10L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1000));

    public static void main(String[] args) throws IOException {
        System.out.println("系统CPU核数："+CORE_POOL_SIZE);
        /*
         * 正则 匹配页码
         */
        String regEx="[^0-9]";
        Pattern p = Pattern.compile(regEx);
        String baseUrl = "https://www.tupianzj.com";
        Connection connect = Jsoup.connect(baseUrl+"/meinv/meizitu/");
        Document document = connect.get();

        Elements elements = document.body().getElementsByClass("d1").select("li");
        for (Element img : elements) {
            Runnable task = () -> {
                try {

                    String href = img.child(0).attr("href");
                    Connection subConnect = Jsoup.connect(baseUrl+href)
                            . header("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:49.0) Gecko/20100101 Firefox/49.0")
                            .timeout(8000);
                    Document subDocument = subConnect.get();

                    String title = subDocument.body().getElementsByTag("h1").eq(1).html();
                    System.out.println("开始下载："+title);

                    String txt =  subDocument.body().getElementsByClass("pages").select("li").eq(0).html();
                    Matcher m = p.matcher(txt);
                    if(StringUtils.isNoneBlank(m.replaceAll("").trim())){
                        int pageNo =  Integer.parseInt(m.replaceAll("").trim());
                        for (int i=0;i<pageNo;i++){
                            String url = baseUrl+href;
                            if(i!=0){
                                int page = i+1;
                                url = url.replace(".html","_"+page+".html");
                            }
                            subConnect = Jsoup.connect(url);
                            subDocument = subConnect.get();
                            String src = Objects.requireNonNull(subDocument.getElementById("bigpicimg")).attr("src");

                            HttpUtil.downloadFile(src, FileUtil.mkdir("e:/图/"+title+"/"));
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            };
            EXECUTOR.execute(task);
        }
    }
}
