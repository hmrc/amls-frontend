package models.msb

import models.registrationprogress.{Completed, Started, NotStarted, Section}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.http.cache.client.CacheMap


class MsbSpec extends PlaySpec with MockitoSugar {

  val completeModel = Msb(None)

  "Msb" must {

    "msp complete model" should {

      "Serialize successfully" in {

      }

      "De-Serialize successfully" in {

      }
    }

    "Section" should {

        implicit val cachMap = mock[CacheMap]

      "navigate to what you need when model is incomplete" in {

        val incompleteModel = mock[Msb]
        val section = Section("msb", Started, controllers.msb.routes.WhatYouNeedController.get())
        when(cachMap.getEntry[Msb]("msb")) thenReturn Some(incompleteModel)

        Msb.section must be(section)
      }

      "navigate to summary when model is complete" in {

        val completeModel = Msb(Some("test"))
        val section = Section("msb", Completed, controllers.msb.routes.SummaryController.get())
        when(cachMap.getEntry[Msb]("msb")) thenReturn Some(completeModel)

        Msb.section must be(section)
      }

      "navigate to summary when model is not started" in {

        val notStartedSection = Section("msb", NotStarted, controllers.msb.routes.WhatYouNeedController.get())
        when(cachMap.getEntry[Msb]("msb")) thenReturn None

        Msb.section must be(notStartedSection)

      }
    }
  }
}