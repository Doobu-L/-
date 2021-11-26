- openssl 발급과정            참고 : https://heodolf.tistory.com/94
- Spring 에서 SSL/TLS 구현    참고 : https://springboot.cloud/20

-Spring Boot 에서 mTLS설정과 API 호출하기

1. 인증서 (cer,crt,key)를 pkcs12 로 변환
2. application.yml 에 mtls 설정

mtls:
  ssl:
    key-store-type: pkcs12
    key-store: classpath:clientCert/mydataClient.p12
    key-store-password: Qodzb123!   //key-store 생성 시 입력한 password
  
  
3. pom.xml 에 dependency 추가
<dependency>
	<groupId>org.apache.httpcomponents</groupId>
	<artifactId>httpclient</artifactId>
	<version>4.5.13</version>
</dependency>

4.p12 파일 확장자 maven에서 필터링 제외(pom.xml)
<plugin>
	<groupId>org.apache.maven.plugins</groupId>
	<artifactId>maven-resources-plugin</artifactId>
	<configuration><encoding>UTF-8</encoding>
		<nonFilteredFileExtensions>
			<nonFilteredFileExtension>p12</nonFilteredFileExtension>
		</nonFilteredFileExtensions>
	</configuration>
</plugin>

4.. RestTemplate Bean 설정(NaverAuthRestTemplate)

SSLConfiguration.java 에 Bean 설정

 

@Configuration
public class SSLConfiguration {

  @Value("${mtls.ssl.key-store-password}")
  private String keyStorePassword;
  @Value("${mtls.ssl.key-store}")
  private Resource keyStore;

  private static final String[] PROTOCOL = new String[]{"TLSv1.3"};
  private static final String[] CIPHER_SUITE = new String[]{"TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384","TLS_AES_128_GCM_SHA256"};

  @Bean
  public NaverAuthRestTemplate naverRestTemplate() throws Exception {
    RestTemplate naverAuthRestTemplate = new RestTemplate(clientHttpRequestFactory());
    return new NaverAuthRestTemplate(naverAuthRestTemplate);  //NaverAuthRestTemplate에 생성자 추가
  }


  private ClientHttpRequestFactory clientHttpRequestFactory() throws Exception {
    return new HttpComponentsClientHttpRequestFactory(mtlsHttpClient());
  }

  private HttpClient mtlsHttpClient() throws Exception {
    SSLContext sslcontext =
        SSLContexts.custom()
            .loadKeyMaterial(keyStore.getFile(), keyStorePassword.toCharArray(), keyStorePassword.toCharArray())
            .build();
    sslcontext.getProtocol();

    SSLConnectionSocketFactory sslConnectionSocketFactory =
        new SSLConnectionSocketFactory(
            sslcontext,
            PROTOCOL,
            CIPHER_SUITE,
            SSLConnectionSocketFactory.getDefaultHostnameVerifier()
        );
    return HttpClients.custom().setSSLSocketFactory(sslConnectionSocketFactory).build();
  }
}


5. 네이버 [통합인증-101] API 요청

URI - /oauth/2.0/token

METHOD - POST

CONTENT TYPE - application/x-www-form-urlencoded

 

네이버 통합인증API -101 은 Content-type application/x-www-form-urlencoded

 

Content type 헤더설정

httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);​
 

application/x-www-form-urlencoded 은 서버단에서 컨버팅 필요

 
MultiValueMap<String, String> convert(Object obj) {
  MultiValueMap parameters = new LinkedMultiValueMap<String, String>();
  Map<String, String> maps = objectMapper.convertValue(obj, new TypeReference<Map<String, String>>() {});
  parameters.setAll(maps);

  return parameters;
}
 

NaverAuthService.java

public class NaverAuthService {
  private final NaverAuthRestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  private final String TOKEN_PATH = "/oauth/2.0/token";
  private final String SIGNREQUEST_PATH = "/v1/ca/sign_request";

  @Value("${naver.clientId}")
  private String naverClientId;

  @Value("${naver.clientSecret}")
  private String naverClientSecret;

public NaverTokenResponse getToken() {
    NaverTokenResponse token_response = null;
    try {
      TokenRequest tokenRequest = new TokenRequest();

      HttpHeaders httpHeaders = new HttpHeaders();
      httpHeaders.set("x-api-tran-id", "4048801311MA"+ DateUtils.to_date());
      httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

      MultiValueMap<String, String> requestMap =  convert(tokenRequest);

      ResponseEntity<NaverTokenResponse> response = restTemplate.post(TOKEN_PATH , httpHeaders,requestMap ,NaverTokenResponse.class);
      token_response = response.getBody();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return token_response;
  }
 

response

{"token_type":"Bearer","access_token":"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJaWUFBUUgwMDAwIiwiYXVkIjoialVZX3ViSUdBRnU0bGdXZGJkQ1YiLCJqdGkiOiJ0b2tlbmlkLWJldGEtb25seSIsImV4cCI6MTYzNzczOTkzNCwic2NvcGUiOiJjYSJ9.9csTOzAXQLt0YGa__HwKrnHuSSvItzYJmBPzRngYKCg","expires_in":604800,"scope":"ca"}​
 
 

mTLS 오류 API 응답 예시

HTTPS STATUS: 403 Forbidden { "rsp_code": "40301", "rsp_msg": "invalid client certificate" }
 


