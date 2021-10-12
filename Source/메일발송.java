/**
 사전작업 
 네이버 ->메일 ->내 메일함 (오른쪽 톱니바퀴모양) ->POP3/SMTP 사용  사용함으로 체크.
 
 라이브러리 :: http://mvnrepository.com/artifact/javax.mail/mail/1.4.7
 참고 :: https://ktko.tistory.com/entry/JAVA-SMTP%EC%99%80-Mail-%EB%B0%9C%EC%86%A1%ED%95%98%EA%B8%B0Google-Naver
 
*/

@GetMapping("/mail")
  public String SMTPTest(@RequestParam String title,@RequestParam String content){
    final String user = "point455@naver.com";
    final String password="";

    Properties prop = new Properties();
    prop.put("mail.smtp.host","smtp.naver.com");
    prop.put("mail.smtp.port",465);
    prop.put("mail.smtp.auth","true");
    prop.put("mail.smtp.ssl.enable","true");
    prop.put("mail.smtp.ssl.trust","smtp.naver.com");

    Session session = Session.getDefaultInstance(prop, new javax.mail.Authenticator() {
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(user, password);
      }
    });
    try{
      MimeMessage message = new MimeMessage(session);
      message.setFrom(new InternetAddress(user));
      //수신자메일주소
      message.addRecipient(RecipientType.TO,new InternetAddress("point455@bankq.co.kr"));

      //Subject
      message.setSubject(title);

      message.setText(content);

      //send the message
      Transport.send(message);
      System.out.printf("message Sent Successfully!!!!!!!!!!!!!!!");


    } catch (AddressException e) {
      e.printStackTrace();
    } catch (MessagingException e) {
      e.printStackTrace();
    }

    return "";

  }
