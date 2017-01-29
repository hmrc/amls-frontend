package utils

import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.MessagesApi
import play.api.test.FakeRequest
import play.api.{Mode, Application}
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.filters.csrf.CSRF.Token
import play.filters.csrf.{CSRFFilter, CSRFConfigProvider}

trait GenericTestHelper extends PlaySpec with OneAppPerSuite {

  private val bindModules: Seq[GuiceableModule] = Seq()

  implicit override lazy val app: Application = new GuiceApplicationBuilder()
    .bindings(bindModules:_*).in(Mode.Test)
    .build()

  implicit val messagesApi = app.injector.instanceOf[MessagesApi]
  implicit val messages = messagesApi.preferred(FakeRequest())

  def addToken[T](fakeRequest: FakeRequest[T]) = {
    val csrfConfig     = app.injector.instanceOf[CSRFConfigProvider].get
    val csrfFilter     = app.injector.instanceOf[CSRFFilter]
    val token          = csrfFilter.tokenProvider.generateToken

    fakeRequest.copyFakeRequest(tags = fakeRequest.tags ++ Map(
      Token.NameRequestTag  -> csrfConfig.tokenName,
      Token.RequestTag      -> token
    )).withHeaders((csrfConfig.headerName, token))
  }

}
