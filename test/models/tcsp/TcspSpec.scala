package models.tcsp

import models.registrationprogress._
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.CacheMap


trait TcspValues {

  object DefaultValues {

    val DefaultProvidedServices = ProvidedServices(Set(PhonecallHandling))

  }

  object NewValues {

    val NewProvidedServices = ProvidedServices(Set(EmailHandling))

  }

  val completeJson = Json.obj()
  val completeTcsp = Tcsp(providedServices = Some(DefaultValues.DefaultProvidedServices))

}

class TcspSpec extends PlaySpec with MockitoSugar with TcspValues {

  "Tcsp" must {

    "have a default function that" must {

      "correctly provides a default value when none is provided" in {
        Tcsp.default(None) must be (Tcsp())
      }

      "correctly provides a default value when existing value is provided" in {
        Tcsp.default(Some(completeTcsp)) must be (completeTcsp)
      }

    }

    "have a mongo key that" must {
      "be correctly set" in {
        Tcsp.mongoKey() must be ("tcsp")
      }
    }

    "have a section function that" must {

      implicit val cache = mock[CacheMap]

      "return a NotStarted Section when model is empty" in {

        val notStartedSection = Section("tcsp", NotStarted, controllers.tcsp.routes.WhatYouNeedController.get())

        when (cache.getEntry[Tcsp]("tcsp")) thenReturn None

        Tcsp.section must be (notStartedSection)

      }

      "return a Completed Section when model is complete" in {

        val complete = mock[Tcsp]
        val completedSection = Section("tcsp", Completed, controllers.routes.RegistrationProgressController.get())

        when (complete.isComplete) thenReturn true
        when (cache.getEntry[Tcsp]("tcsp")) thenReturn Some(complete)

        Tcsp.section must be (completedSection)

      }

      "return a Started Section when model is incomplete" in {

        val incompleteTcsp = mock[Tcsp]
        val startedSection = Section("tcsp", Started, controllers.tcsp.routes.WhatYouNeedController.get())

        when(incompleteTcsp.isComplete) thenReturn false
        when(cache.getEntry[Tcsp]("tcsp"))thenReturn Some(incompleteTcsp)

        Tcsp.section must be (startedSection)

      }
    }

    "have an isComplete function that" must {

      "correctly show if the model is complete" in {
        completeTcsp.isComplete must be (true)
      }

      "correctly show if the model is not complete" in {
        val incomplete = completeTcsp.copy(providedServices = None)
        incomplete.isComplete must be (false)
      }

    }

    "correctly convert between json formats" must {

      "Serialise as expected" in {
        Json.toJson(completeTcsp) must be(completeJson)
      }

      "Deserialise as expected" in {
        completeJson.as[Tcsp] must be(completeTcsp)
      }

    }

  }

}
