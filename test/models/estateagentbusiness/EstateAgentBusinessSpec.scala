package models.estateagentbusiness

import models.registrationprogress.{Completed, NotStarted, Section, Started}
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

  val newServices = Services(Set(Commercial, Relocation))
  val newProfessionalBody = ProfessionalBodyNo
  val newPenalisedEAAct = PenalisedUnderEstateAgentsActNo
  val newRedressScheme = OmbudsmanServices

  val completeJson = Json.obj(
    "services" -> Json.obj(
      "services" -> Seq("01", "02", "03")
    ),
    "redressScheme" -> Json.obj(
      "isRedress" -> true,
      "propertyRedressScheme" -> "04",
      "propertyRedressSchemeOther" -> "test"
    ),
    "penalisedUnderEstateAgentsAct" -> Json.obj(
      "penalisedUnderEstateAgentsAct" -> true,
      "penalisedUnderEstateAgentsActDetails" -> "test"
    ),
    "professionalBody" -> Json.obj(
      "penalised" -> true,
      "professionalBody" -> "details"
    ),
    "hasChanged" -> false
  )

  val completeModel = EstateAgentBusiness(
    services = Some(services),
    redressScheme =  Some(redressSchemeOther),
    professionalBody = Some(professionalBody),
    penalisedUnderEstateAgentsAct = Some(penalisedUnderEAAct)
  )

  val incompleteModel = EstateAgentBusiness(
    services = None,
    redressScheme = None,
    professionalBody = None,
    penalisedUnderEstateAgentsAct = None
  )

  "EstateAgentBusiness" must {
    "validate complete json" must {
      "Serialise as expected" in {
        Json.toJson(completeModel) must
          be(completeJson)
      }

      "Deserialise as expected" in {
        (completeJson  - "hasChanged").as[EstateAgentBusiness] must
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
        result must be (EstateAgentBusiness(Some(services), None, hasChanged = true))
      }
    }

    "Merged with RedressScheme" must {
      "return EstateAgentBusiness with correct redressScheme" in {
        val result = initial.redressScheme(redressSchemeOther)
        result must be (EstateAgentBusiness(None,Some(redressSchemeOther), None, hasChanged = true))
      }
    }
    "Merged with professionalBody" must {
      "return EstateAgentBusiness with correct professionalBody" in {
        val result = initial.professionalBody(professionalBody)
        result must be (EstateAgentBusiness(None, None, Some(professionalBody), None, hasChanged = true))
      }
    }

    "Merged with penalisedUnderEAAct" must {
      "return EstateAgentBusiness with correct penalisedUnderEAAct" in {
        val result = initial.penalisedUnderEstateAgentsAct(penalisedUnderEAAct)
        result must be (EstateAgentBusiness(None, None, None, Some(penalisedUnderEAAct), hasChanged = true))
      }
    }
  }

  "Merge to the model" must {

    val initial = EstateAgentBusiness(Some(services), Some(redressSchemeOther), Some(professionalBody), Some(penalisedUnderEAAct))

    "Merged with services" must {
      "return EstateAgentBusiness with correct business services" in {
        val result = initial.services(newServices)
        result must be (EstateAgentBusiness(Some(newServices),  Some(redressSchemeOther), Some(professionalBody), Some(penalisedUnderEAAct), hasChanged = true))
      }
    }

    "Merged with redressScheme" must {
      "return EstateAgentBusiness with correct redressScheme" in {
        val result = initial.redressScheme(newRedressScheme)
        result must be (EstateAgentBusiness(Some(services),  Some(newRedressScheme), Some(professionalBody), Some(penalisedUnderEAAct), hasChanged = true))
      }
    }

    "Merged with professionalBody" must {
      "return EstateAgentBusiness with correct professionalBody" in {
        val result = initial.professionalBody(newProfessionalBody)
        result must be (EstateAgentBusiness(Some(services), Some(redressSchemeOther), Some(newProfessionalBody), Some(penalisedUnderEAAct), hasChanged = true))
      }
    }

    "Merged with penalisedUnderEAAct" must {
      "return EstateAgentBusiness with correct penalisedUnderEAAct" in {
        val result = initial.penalisedUnderEstateAgentsAct(newPenalisedEAAct)
        result must be (EstateAgentBusiness(Some(services), Some(redressSchemeOther), Some(professionalBody), Some(newPenalisedEAAct), hasChanged = true))
      }
    }
  }

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
      EstateAgentBusiness.section mustBe Section("eab", Completed, false, controllers.estateagentbusiness.routes.SummaryController.get(true))

    }
  }

  "EstateAgentBusiness class" when {
    "services value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val res = completeModel.services(services)
          res must be(completeModel)
          res.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val res = completeModel.services(newServices)
          res.hasChanged must be(true)
          res.services must be(Some(newServices))
        }
      }
    }

    "penalisedUnderEstateAgentsAct value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val res = completeModel.penalisedUnderEstateAgentsAct(penalisedUnderEAAct)
          res must be(completeModel)
          res.hasChanged must be(false)
        }
      }
      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val res = completeModel.penalisedUnderEstateAgentsAct(newPenalisedEAAct)
          res.hasChanged must be(true)
          res.penalisedUnderEstateAgentsAct must be(Some(newPenalisedEAAct))
        }
      }
    }

    "professionalBody value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val res = completeModel.professionalBody(professionalBody)
          res must be(completeModel)
          res.hasChanged must be(false)
        }
      }
      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val res = completeModel.professionalBody(newProfessionalBody)
          res.hasChanged must be(true)
          res.professionalBody must be(Some(newProfessionalBody))
        }
      }
    }

    "redressScheme value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val res = completeModel.redressScheme(redressSchemeOther)
          res must be(completeModel)
          res.hasChanged must be(false)
        }
      }
      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val res = completeModel.redressScheme(newRedressScheme)
          res.hasChanged must be(true)
          res.redressScheme must be(Some(newRedressScheme))
        }
      }
    }

  }

}
