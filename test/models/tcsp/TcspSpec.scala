package models.tcsp

import models.registrationprogress.{Started, NotStarted, Section}
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.CacheMap


trait TcspValues {

  object DefaultValues {
    val DefaultCompanyServiceProviders = TcspTypes(Set(NomineeShareholdersProvider, TrusteeProvider,
      CompanyDirectorEtc, CompanyFormationAgent(true, false)))
  }

  object NewValues {
    val NewCompanyServiceProviders = TcspTypes(Set(NomineeShareholdersProvider, CompanyFormationAgent(true, false)))
  }

  val completeJson = Json.obj(
    "serviceProviders[]" -> Seq("01", "02", "04"),
    "onlyOffTheShelfCompsSold" -> Seq("true"),
    "complexCorpStructureCreation" -> Seq("false")
  )

  val completeModel = Tcsp(Some(DefaultValues.DefaultCompanyServiceProviders))
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

      "return a Started Section when model is incomplete" in {

        val incompleteTcsp = mock[Tcsp]
        val startedSection = Section("tcsp", Started, controllers.tcsp.routes.WhatYouNeedController.get())

        when(incompleteTcsp.isComplete) thenReturn false
        when(cache.getEntry[Tcsp]("tcsp"))thenReturn Some(incompleteTcsp)

        Tcsp.section must be (startedSection)

      }
    }

    "Complete Model" when {

      "correctly convert between json formats" when {

        "Serialise as expected" ignore {
          Json.toJson(completeModel) must be(completeJson)
        }

        "Deserialise as expected" ignore {
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
    }

    "Tcsp:merge with completeModel" when {
      val initial = Tcsp(Some(DefaultValues.DefaultCompanyServiceProviders))

      "model is complete" when {

        "Merged with Company Service Providers" must {
          "return Tcsp with correct Company Service Providers" in {
            val result = initial.tcspTypes(NewValues.NewCompanyServiceProviders)
            result must be(Tcsp(tcspTypes = Some(NewValues.NewCompanyServiceProviders)))
          }
        }
      }

    }
  }
}
