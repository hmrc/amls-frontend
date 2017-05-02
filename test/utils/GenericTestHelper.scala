package utils

import connectors.DataCacheConnector
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.MessagesApi
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.{Application, Mode}
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.filters.csrf.CSRF.Token
import play.filters.csrf.{CSRFConfigProvider, CSRFFilter}

trait GenericTestHelper extends PlaySpec with OneAppPerSuite with MockitoSugar {

  protected val bindModules: Seq[GuiceableModule] = Seq()

  val dataCacheConnector = mock[DataCacheConnector]
  implicit override lazy val app: Application = new GuiceApplicationBuilder()
    .bindings(bindModules:_*).in(Mode.Test)
    .overrides(bind[DataCacheConnector].to(dataCacheConnector))
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
