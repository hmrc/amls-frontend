package models.estateagentbusiness

import models.registrationprogress.{NotStarted, Section, Started}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.CacheMap
import org.mockito.Mockito._
import org.mockito.Matchers.{eq => eqTo, _}
import uk.gov.hmrc.play.frontend.auth.AuthContext

class EstateAgentBusinessSpec extends PlaySpec with MockitoSugar {

  val services = Services(Set(Residential, Commercial, Auction))
  val professionalBody = ProfessionalBodyYes("details")
  val penalisedUnderEAAct =  PenalisedUnderEstateAgentsActYes("test")
  val redressSchemeOther = Other("test")

  "EstateAgentBusiness" must {

    "validate complete json" must {
      val completeJson = Json.obj(
        "services" -> Seq("01", "02","03"),
        "isRedress" -> true,
        "propertyRedressScheme" -> "04",
        "propertyRedressSchemeOther" -> "test",
        "penalised" -> true,
        "professionalBody" -> "details",
        "penalisedUnderEstateAgentsAct" -> true,
        "penalisedUnderEstateAgentsActDetails" -> "test",
        "hasChanged" -> true
      )

      val completeModel = EstateAgentBusiness(
        services = Some(services),
        redressScheme =  Some(redressSchemeOther),
        professionalBody = Some(professionalBody),
        penalisedUnderEstateAgentsAct = Some(penalisedUnderEAAct),
        hasChanged = true
      )

      "Serialise as expected" in {
        Json.toJson(completeModel) must
          be(completeJson)
      }

      "Deserialise as expected" in {
        completeJson.as[EstateAgentBusiness] must
          be(completeModel)
      }
    }
  }

  "Partially complete EstateAgentBusiness" must {
    "convert ProfessionalBody to `EAB`" in {

      val model = EstateAgentBusiness(None,
                                      None,
                                      Some(ProfessionalBodyYes("details")))
      val zero = EstateAgentBusiness(None,
                                     None,
                                     None)

      (Some(model): EstateAgentBusiness) must be(model)
      (None: EstateAgentBusiness) must be(zero)
    }
  }

  "None" must {
    val initial: Option[EstateAgentBusiness] = None

    "Merged with services" must {
      "return EstateAgentBusiness with correct business services" in {
        val result = initial.services(services)
        result must be (EstateAgentBusiness(Some(services), None))
      }
    }

    "Merged with RedressScheme" must {
      "return EstateAgentBusiness with correct redressScheme" in {
        val result = initial.redressScheme(redressSchemeOther)
        result must be (EstateAgentBusiness(None,Some(redressSchemeOther), None))
      }
    }
    "Merged with professionalBody" must {
      "return EstateAgentBusiness with correct professionalBody" in {
        val result = initial.professionalBody(professionalBody)
        result must be (EstateAgentBusiness(None, None, Some(professionalBody), None))
      }
    }

    "Merged with penalisedUnderEAAct" must {
      "return EstateAgentBusiness with correct penalisedUnderEAAct" in {
        val result = initial.penalisedUnderEstateAgentsAct(penalisedUnderEAAct)
        result must be (EstateAgentBusiness(None, None, None, Some(penalisedUnderEAAct)))
      }
    }
  }

  "Merge to the model" must {

    val initial = EstateAgentBusiness(Some(services), Some(redressSchemeOther), Some(professionalBody), Some(penalisedUnderEAAct))

    "Merged with services" must {
      "return EstateAgentBusiness with correct business services" in {
        val newServices = Services(Set(Commercial, Auction, Residential))
        val result = initial.services(newServices)
        result must be (EstateAgentBusiness(Some(newServices),  Some(redressSchemeOther), Some(professionalBody), Some(penalisedUnderEAAct), hasChanged = true))
      }
    }

    "Merged with redressScheme" must {
      "return EstateAgentBusiness with correct redressScheme" in {
        val newRedressScheme = OmbudsmanServices
        val result = initial.redressScheme(newRedressScheme)
        result must be (EstateAgentBusiness(Some(services),  Some(newRedressScheme), Some(professionalBody), Some(penalisedUnderEAAct)))
      }
    }

    "Merged with professionalBody" must {
      "return EstateAgentBusiness with correct professionalBody" in {
        val newProfessionalBody = ProfessionalBodyNo
        val result = initial.professionalBody(newProfessionalBody)
        result must be (EstateAgentBusiness(Some(services), Some(redressSchemeOther), Some(newProfessionalBody), Some(penalisedUnderEAAct)))
      }
    }

    "Merged with penalisedUnderEAAct" must {
      "return EstateAgentBusiness with correct penalisedUnderEAAct" in {
        val newPenalisedEAAct = PenalisedUnderEstateAgentsActNo
        val result = initial.penalisedUnderEstateAgentsAct(newPenalisedEAAct)
        result must be (EstateAgentBusiness(Some(services), Some(redressSchemeOther), Some(professionalBody), Some(newPenalisedEAAct)))
      }
    }
  }

  val completeModel = EstateAgentBusiness(
    services = Some(Services(Set(Residential))),
    redressScheme = Some(ThePropertyOmbudsman),
    professionalBody = Some(ProfessionalBodyNo),
    penalisedUnderEstateAgentsAct = Some(PenalisedUnderEstateAgentsActNo)
  )

  val incompleteModel = EstateAgentBusiness(
    services = None,
    redressScheme = None,
    professionalBody = None,
    penalisedUnderEstateAgentsAct = None
  )

  "isComplete" must {

    "return true when all internal properties are `Some`" in {
      completeModel.isComplete mustEqual true
    }

    "return true when there is no redress scheme and no Residential service" in {
      val model = EstateAgentBusiness(
        services = Some(Services(Set.empty)),
        redressScheme = None,
        professionalBody = Some(ProfessionalBodyNo),
        penalisedUnderEstateAgentsAct = Some(PenalisedUnderEstateAgentsActNo)
      )

      model.isComplete mustEqual true
    }

    "return false when properties are `None`" in {
      incompleteModel.isComplete mustEqual false
    }
  }

  "section" must {

    "return `NotStarted` section when there is no section in s4l" in {
      implicit val cache = CacheMap("", Map.empty)
      EstateAgentBusiness.section mustBe Section("eab", NotStarted, false,  controllers.estateagentbusiness.routes.WhatYouNeedController.get)
    }

    "return `Started` section when there is a section which isn't completed" in {
      implicit val cache = mock[CacheMap]
      implicit val ac = mock[AuthContext]
      when {
        cache.getEntry[EstateAgentBusiness](eqTo(EstateAgentBusiness.key))(any())
      } thenReturn Some(incompleteModel)
      EstateAgentBusiness.section mustBe Section("eab", Started, false, controllers.estateagentbusiness.routes.WhatYouNeedController.get)
    }

    "return `Completed` section when there is a section which is completed" in {
      implicit val cache = mock[CacheMap]
      implicit val ac = mock[AuthContext]
      when {
        cache.getEntry[EstateAgentBusiness](eqTo(EstateAgentBusiness.key))(any())
      } thenReturn Some(completeModel)
    }
  }
}
