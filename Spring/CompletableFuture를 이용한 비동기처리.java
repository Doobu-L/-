##테스트를 위해 임시로 만든 서비스
# https://codechacha.com/ko/java-completable-future/ 
#Service 
#목적 - CompletableFuture를 이용한 비동기 처리.
        mydataApiCardService.updateAssetUserSearch 를 비동기로 호출 후 응답을 기다리고 다음 작업 수행.(블락)
        futures를 비동기 요청을 통해 수집 -> CompletableFuture.allOf().join() (응답을 취합 - 블락)
        이 후 아래 로직 수행.

 public void update(BaseMydataUserRequest req) {
      List<UserOauth> userOauthList = userOauthRepository.findByMydataUserIdAndIsSync(req.getId(),"Y");
      ExecutorService executor = Executors.newFixedThreadPool(10);

      CompletableFuture[] futures = IntStream.range(0,10).mapToObj(a -> {
        log.info(" :: "+a);
        return CompletableFuture.supplyAsync(()->mydataApiCardService.updateAssetUserSearch(a),executor);

        /*if(a.getOrgCode().startsWith("D1"))
          return mydataApiCardService.updateAssetUserSearch(req, a.getOrgCode());
        else
          return mydataApiAccountService.updateAccountUserSearch(req,a.getOrgCode());*/

      }).toArray(CompletableFuture[]::new);

      CompletableFuture.allOf(futures).join();

      log.info("SUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUCCCCCCCCCCCCC!!!!!!");
      Calendar cal = Calendar.getInstance();
      cal.setTime(new Date());
      cal.add(Calendar.DATE, -28);

      /*for(int i = 1 ;  i < 5; i ++){
        log.info("레포트!!!");
        cal.add(Calendar.DATE, 7);
        consumptionReportService.update(req, DateUtils.SDF_yyyyMMdd.format(cal.getTime()));
      }*/

    }


  #외부 클래스에 있는 비동기 요청 로직. 
  #기존에는 @Async로 되어있었는데 CompletableFuture를 통해 비동기 수행으로 변경.
    
	@SuppressWarnings("rawtypes")
	//@Async
	public CompletableFuture<Boolean> updateAssetUserSearch(int num) {
		try {
			log.info("start :: "+num + " --- " +Thread.currentThread().getName());
			Thread.sleep(1);
			log.info("end :: "+num + " --- " +Thread.currentThread().getName());
			return CompletableFuture.completedFuture(Boolean.TRUE);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return CompletableFuture.completedFuture(Boolean.FALSE);

	}

