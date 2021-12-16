package kr.co.bankq.mydata.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import kr.co.bankq.mydata.domain.dto.ConsentDetailDto;
import kr.co.bankq.mydata.domain.dto.ConsentDto;
import kr.co.bankq.mydata.domain.dto.NaverSignRequestDto;
import kr.co.bankq.mydata.domain.dto.NaverSignResultDto;
import kr.co.bankq.mydata.domain.entity.bankq.User;
import kr.co.bankq.mydata.domain.entity.mydata.MydataUser;
import kr.co.bankq.mydata.domain.entity.mydata.Org;
import kr.co.bankq.mydata.domain.response.CommonResponse;
import kr.co.bankq.mydata.domain.response.NaverSignRequestResponse;
import kr.co.bankq.mydata.domain.response.NaverSignResultResponse;
import kr.co.bankq.mydata.domain.response.NaverTokenResponse;
import kr.co.bankq.mydata.exception.InvalidMydataUserException;
import kr.co.bankq.mydata.repository.bankq.UserRepository;
import kr.co.bankq.mydata.repository.mydata.MydataUserRepository;
import kr.co.bankq.mydata.repository.mydata.OrgRepository;
import kr.co.bankq.mydata.util.DateUtils;
import kr.co.bankq.mydata.util.GenerateCode;
import kr.co.bankq.mydata.util.NaverAuthRestTemplate;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;

@Service
@RequiredArgsConstructor
public class NaverAuthService {

  private final NaverAuthRestTemplate restTemplate;
  private final ObjectMapper objectMapper;
  private final MydataUserRepository mydataUserRepository;
  private final UserRepository userRepository;
  private final OrgRepository orgRepository;


  private final String TOKEN_PATH = "/oauth/2.0/token";
  private final String SIGNREQUEST_PATH = "/v1/ca/sign_request";
  private final String SIGNRESULT_PATH = "/v1/ca/sign_result";

  @Value("${naver.clientId}")
  private String naverClientId;

  @Value("${naver.clientSecret}")
  private String naverClientSecret;

  @Value("${mydata.bankqOrgCode}")
  private String bankqOrgCode;

  @Value("${naver.orgCode}")
  private String naverOrgCode;

  @Value("${naver.return-app-scheme-url}")
  private String returnAppSchemeUrl;

  public ResponseEntity<?> issueTokenForNaverAuth() {
    NaverTokenResponse token_response = null;
    try {
      TokenRequest tokenRequest = new TokenRequest();

      HttpHeaders httpHeaders = new HttpHeaders();
      httpHeaders.set("x-api-tran-id", GenerateCode.generateApiTranId());
      httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

      //Content-type이 json 이 아니고 x-www-form-urlencoded 이기 때문에 MultiValueMap으로 Convert
      MultiValueMap<String, String> requestMap =  convert(tokenRequest);

      ResponseEntity<NaverTokenResponse> response = restTemplate.post(TOKEN_PATH , httpHeaders,requestMap ,NaverTokenResponse.class);
      token_response = response.getBody();

    }catch (HttpClientErrorException e){
      return ResponseEntity.status(e.getStatusCode()).body(new CommonResponse(e.getStatusCode().toString(),e.getMessage()));
    }catch (Exception e) {
      e.printStackTrace();
    }
    return ResponseEntity.ok().body(token_response);
  }

  /*
  * 전자서명요청
  * 1. 1차전 송요구내역원문(ConsentDetailDto) 만들어서 SHA-256으로 인코딩하고
  * 2. ConsentDto 의 consent 에 세팅
  * 3. 네이버전자서명요청(NaverSignRequestDto) 의 TagetInfo 에 List<ConsentDto> 세팅하고 네이버에 전자서명요청
  * */
  @Transactional
  public ResponseEntity<?> signRequest(long mydataUserId,String nToken, List<String> orgCodes)
      throws Exception {
    MydataUser mydataUser = mydataUserRepository.findById(mydataUserId).orElseThrow(InvalidMydataUserException::new);
    User user = userRepository.findById(mydataUser.getBankqId());
    List<Org> orgList = orgRepository.findAllByOrgCodeIn(orgCodes);

    List<ConsentDetailDto> consentDetailList = new ArrayList<>();
    for(Org org:orgList){
      consentDetailList.add(new ConsentDetailDto(org));
    }

    //1차전송요구내역
    consentDetailList.stream().forEach(consentDetailDto -> {
        consentDetailDto.consent1st(bankqOrgCode); //is_scheduled 임시
      }
    );

    ObjectMapper mapper = new ObjectMapper();
    List<ConsentDto> consentList = new ArrayList<>();
    String jsonConsent = null;

    for(ConsentDetailDto consent : consentDetailList){
      jsonConsent = mapper.writeValueAsString(consent);
      String encConsent= encode(jsonConsent);
      //Todo  encConsent length 7000 이상일때 all_asset
      if(encConsent.length()>7000){
        consent.setAllAsset();
        jsonConsent = mapper.writeValueAsString(consent);
        encConsent=encode(jsonConsent);
      }
      consentList.add(new ConsentDto(consent.getIndustry(),encConsent,issueConsentTxId(mydataUserId,consent.getSnd_org_code(),consentList.size()))); //consent_title 어떻게할지?
    }

    NaverSignRequestDto naverSignRequest = new NaverSignRequestDto(issueSignTxId(user.getId()),user,consentList,returnAppSchemeUrl);

    //Todo invalid Token Exception
    //NaverTokenResponse tokenResponse = (NaverTokenResponse) issueTokenForNaverAuth().getBody();
    ResponseEntity<NaverSignRequestResponse> response = null;
    try {
      HttpHeaders httpHeaders = new HttpHeaders();
      httpHeaders.set("x-api-tran-id", GenerateCode.generateApiTranId());
      httpHeaders.setBearerAuth(nToken);
      response = restTemplate.post(SIGNREQUEST_PATH , httpHeaders, naverSignRequest ,NaverSignRequestResponse.class);
      response.getBody().setSign_tx_id(naverSignRequest.getSign_tx_id());
    }catch (HttpClientErrorException e){
      return ResponseEntity.status(e.getStatusCode()).body(e.getMessage());
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }
    return ResponseEntity.ok().body(response.getBody());
  }

  @Transactional
  public ResponseEntity<?> signResult(String nToken, NaverSignResultDto naverSignResultRequest){
    ResponseEntity<NaverSignResultResponse> response = null;

    try{
      HttpHeaders httpHeaders = new HttpHeaders();
      httpHeaders.set("x-api-tran-id", GenerateCode.generateApiTranId());
      httpHeaders.setBearerAuth(nToken);
      response = restTemplate.post(SIGNRESULT_PATH,httpHeaders,naverSignResultRequest,NaverSignResultResponse.class);

    }catch (HttpClientErrorException e){
      return ResponseEntity.status(e.getStatusCode()).build();
    }catch (Exception e){
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
    return ResponseEntity.ok().body(response.getBody());
  }

  @Data
  private class TokenRequest{
    private String grant_type = "client_credentials";
    private String scope = "ca";
    private String client_id = naverClientId;
    private String client_secret = naverClientSecret;
  }

  private MultiValueMap<String, String> convert(Object obj) {
    MultiValueMap parameters = new LinkedMultiValueMap<String, String>();
    Map<String, String> maps = objectMapper.convertValue(obj, new TypeReference<Map<String, String>>() {});
    parameters.setAll(maps);

    return parameters;
  }

  private String issueSignTxId(long id){
    return bankqOrgCode+"_"+naverOrgCode+"_"+DateUtils.today()+"_"+StringUtils.leftPad(id+"",12,"0");
  }

  private String issueConsentTxId(long id,String orgCode, int index){
    return "MD_"+bankqOrgCode+"_"+orgCode+"_"+naverOrgCode+"_"+DateUtils.today()+"_"+StringUtils.leftPad(id+index+"",12,"0");
  }

  public String encode(String raw) {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
  }

  public String decode(String input) {
    Base64.Decoder decoder = Base64.getUrlDecoder();
    // Decoding URl
    return new String(decoder.decode(input));
  }

}
