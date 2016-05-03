package models.tcsp

import models.registrationprogress.{Completed, Started, NotStarted, Section}
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.CacheMap


trait TcspValues {

  object DefaultValues {

    val DefaultCompanyServiceProviders = TcspTypes(Set(NomineeShareholdersProvider, TrusteeProvider,
      CompanyDirectorEtc, CompanyFormationAgent(true, false)))
    val DefaultServicesOfAnotherTCSP = ServicesOfAnotherTCSPYes("12345678")
  }

  object NewValues {

    val NewCompanyServiceProviders = TcspTypes(Set(NomineeShareholdersProvider, CompanyFormationAgent(true, false)))
    val NewServicesOfAnotherTCSP = ServicesOfAnotherTCSPNo
  }

  val completeJson = Json.obj(
    "tcspTypes" -> Json.obj("serviceProviders" -> Seq("01", "02", "04", "05"),
      "onlyOffTheShelfCompsSold" -> true,
      "complexCorpStructureCreation" -> false),
    "servicesOfAnotherTCSP" -> Json.obj("servicesOfAnotherTCSP" -> true,
      "mlrRefNumber" -> "12345678")
  )

  val completeModel = Tcsp(Some(DefaultValues.DefaultCompanyServiceProviders), Some(DefaultValues.DefaultServicesOfAnotherTCSP))
}

class TcspSpec extends PlaySpec with MockitoSugar with TcspValues {

  "Tcsp" must {

    "have a default function that" must {

      "correctly provides a default value when none is provided" in {
        Tcsp.default(None) must be(Tcsp())
      }

      "correctly provides a default value when existing value is provided" in {
        Tcsp.default(Some(completeModel)) must be(completeModel)
      }

    }

    "have a mongo key that" must {
      "be correctly set" in {
        Tcsp.mongoKey() must be("tcsp")
      }
    }

    "have a section function that" must {

      implicit val cache = mock[CacheMap]

      "return a NotStarted Section when model is empty" in {

        val notStartedSection = Section("tcsp", NotStarted, controllers.tcsp.routes.WhatYouNeedController.get())

        when(cache.getEntry[Tcsp]("tcsp")) thenReturn None

        Tcsp.section must be(notStartedSection)

      }

      "return a Completed Section when model is complete" in {

        val complete = mock[Tcsp]
        val completedSection = Section("tcsp", Completed, controllers.routes.RegistrationProgressController.get())

        when(complete.isComplete) thenReturn true
        when(cache.getEntry[Tcsp]("tcsp")) thenReturn Some(complete)

        Tcsp.section must be(completedSection)

      }

      "return a Started Section when model is incomplete" in {

        val incompleteTcsp = mock[Tcsp]
        val startedSection = Section("tcsp", Started, controllers.tcsp.routes.WhatYouNeedController.get())

        when(incompleteTcsp.isComplete) thenReturn false
        when(cache.getEntry[Tcsp]("tcsp")) thenReturn Some(incompleteTcsp)

        Tcsp.section must be(startedSection)

      }
    }

    "have an isComplete function that" must {

      "correctly show if the model is complete" in {
        completeModel.isComplete must be(true)
      }
    }

    "Complete Model" when {

      "correctly convert between json formats" when {

        "Serialise as expected" in {
          Json.toJson(completeModel) must be(completeJson)
        }

        "Deserialise as expected" in {
          completeJson.as[Tcsp] must be(completeModel)
        }
      }
    }

    "None" when {

      val initial: Option[Tcsp] = None

      "Merged with Company Service Providers" must {
        "return Tcsp with correct Company Service Providers" in {
          val result = initial.tcspTypes(NewValues.NewCompanyServiceProviders)
          result must be(Tcsp(tcspTypes = Some(NewValues.NewCompanyServiceProviders)))
        }
      }

      "Merged with services of another tcsp" must {

        "return Tcsp with correct services of another tcsp" in {
          val result = initial.servicesOfAnotherTCSP(NewValues.NewServicesOfAnotherTCSP)
          result must be(Tcsp(servicesOfAnotherTCSP = Some(NewValues.NewServicesOfAnotherTCSP)))
        }
      }
    }

    "Tcsp:merge with completeModel" when {
      val initial = Tcsp(Some(DefaultValues.DefaultCompanyServiceProviders), Some(DefaultValues.DefaultServicesOfAnotherTCSP))

      "model is complete" when {

        "Merged with Company Service Providers" must {
          "return Tcsp with correct Company Service Providers" in {
            val result = initial.tcspTypes(NewValues.NewCompanyServiceProviders)
            result must be(Tcsp(tcspTypes = Some(NewValues.NewCompanyServiceProviders), Some(DefaultValues.DefaultServicesOfAnotherTCSP)))
          }
        }

        "Merged with services of another tcsp" must {
          "return Tcsp with correct services of another tcsp" in {
            val result = initial.servicesOfAnotherTCSP(NewValues.NewServicesOfAnotherTCSP)
            result must be(Tcsp(Some(DefaultValues.DefaultCompanyServiceProviders), servicesOfAnotherTCSP = Some(NewValues.NewServicesOfAnotherTCSP)))
          }
        }
      }

    }
  }
}
