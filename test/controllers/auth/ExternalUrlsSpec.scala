package controllers.auth

import org.scalatestplus.play.{PlaySpec,OneServerPerSuite}

class ExternalUrlsSpec extends PlaySpec with OneServerPerSuite{

  "ExternalUrls" must {

    "have companyAuthHost " in {
      ExternalUrls.companyAuthHost must be("http://localhost:9025")
    }

    "have loginCallback " in {
      ExternalUrls.loginCallback must be("http://localhost:9916/anti-money-laundering/login")
    }

    "have loginPath " in {
      ExternalUrls.loginPath must be("sign-in")
    }

    "have signIn " in {
      ExternalUrls.signIn must be( s"""http://localhost:9025/account/sign-in?continue=http://localhost:9916/anti-money-laundering/login""")
    }

    "have signOut " in {
      ExternalUrls.signOut must be( s"""http://localhost:9025/account/sign-out""")
    }

  }

}
