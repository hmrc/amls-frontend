package utils

import connectors.DataCacheConnector
import models.aboutthebusiness.{AboutTheBusiness, ActivityStartDate, PreviouslyRegisteredNo}
import models.responsiblepeople.{PositionWithinBusiness, Positions, ResponsiblePeople}
import models.tradingpremises.{Address, TradingPremises, YourTradingPremises}
import org.joda.time.LocalDate
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class DateOfChangeHelperSpec extends UnitSpec with MockitoSugar {

  "DateOfChangeHelper" should {
    "return the earliest date from a responsible people, trading premises and about the business" in {

      val aboutTheBusiness = AboutTheBusiness(activityStartDate = Some(ActivityStartDate(new LocalDate(1990, 2, 24))))

      val tradingPremises = Seq(
        TradingPremises(yourTradingPremises = Some(YourTradingPremises(
          "foo",
          Address(
            "1",
            "2",
            None,
            None,
            "asdfasdf"
          ),
          true,
          new LocalDate(1991, 2, 24)
        ))),
        TradingPremises(yourTradingPremises = Some(YourTradingPremises(
          "foo",
          Address(
            "1",
            "2",
            None,
            None,
            "asdfasdf"
          ),
          true,
          new LocalDate(1992, 2, 24)
        ))),
        TradingPremises(yourTradingPremises = Some(YourTradingPremises(
          "foo",
          Address(
            "1",
            "2",
            None,
            None,
            "asdfasdf"
          ),
          true,
          new LocalDate(1993, 2, 24)
        )))
      )

      val responsiblePeople = Seq(
        ResponsiblePeople(positions = Some(Positions(Set.empty[PositionWithinBusiness], Some(new LocalDate(1995, 2, 24)))))
      )

      val mockDataCacheConnector = mock[DataCacheConnector]

      val mockCacheMap = mock[CacheMap]

      when(mockDataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(mockCacheMap.getEntry[AboutTheBusiness](any())(any()))
        .thenReturn(Some(aboutTheBusiness))

      when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
        .thenReturn(Some(responsiblePeople))

      when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
        .thenReturn(Some(tradingPremises))

      object DateOfChangeHelperTest extends DateOfChangeHelper{
        override val dataCacheConnector: DataCacheConnector = mockDataCacheConnector
      }

      implicit val hc: HeaderCarrier = mock[HeaderCarrier]
      implicit val ac: AuthContext = mock[AuthContext]

      await(DateOfChangeHelperTest.compareToStartDate).get should be(LocalDate.now)
    }
  }

}
