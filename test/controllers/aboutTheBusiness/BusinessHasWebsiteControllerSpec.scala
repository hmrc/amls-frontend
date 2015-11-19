package controllers

import java.util.UUID
import connectors.DataCacheConnector
import controllers.aboutTheBusiness.BusinessHasWebsiteController

import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

class BusinessHasWebsiteControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures with BeforeAndAfterEach {

  val userId = s"user-${UUID.randomUUID}"
  val mockAuthConnector = mock[AuthConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]

  object MockBusinessHasWebsiteController extends BusinessHasWebsiteController{
    val authConnector = mockAuthConnector
    override val dataCacheConnector: DataCacheConnector = mockDataCacheConnector
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
  }

  "BusinessHasWebsiteController" must {
//    "on load display the businessHasWebsite page" in {
//      when(mockDataCacheConnector.fetchDataShortLivedCache[BusinessHasWebsite](any())
//        (any(), any(),  any())).thenReturn(Future.successful(None))
//      val result = MockBusinessHasWebsiteController.get(mock[AuthContext], SessionBuilder.buildRequestWithSession(userId))
//      status(result) must be(OK)
//      contentAsString(result) must include("Does your business have a website?")
//    }
  }
}
