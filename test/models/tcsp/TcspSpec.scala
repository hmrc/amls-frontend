package models.tcsp

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json


class TcspSpec extends PlaySpec with MockitoSugar {

  "Tcsp" must {

    val DefaultCompanyServiceProviders = TcspTypes(Set(NomineeShareholdersProvider, TrusteeProvider, CompanyDirectorEtc(true, false)))
    val NewCompanyServiceProviders = TcspTypes(Set(NomineeShareholdersProvider, TrusteeProvider, CompanyDirectorEtc(true, false)))

    "Complete Model" when {

      "correctly convert between json formats" when {

        val completeJson = Json.obj(
          "serviceProviders[]" -> Seq("01", "02", "04"),
          "onlyOffTheShelfCompsSold" -> Seq("true"),
          "complexCorpStructureCreation" -> Seq("false")
        )
        val completeModel = Tcsp(Some(DefaultCompanyServiceProviders))

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
          val result = initial.tcspTypes(NewCompanyServiceProviders)
          result must be(Tcsp(tcspTypes = Some(NewCompanyServiceProviders)))
        }
      }
    }

    "Tcsp" when {
      val initial = Tcsp(Some(DefaultCompanyServiceProviders))

      "model is complete" when {

        "Merged with Company Service Providers" must {
          "return Tcsp with correct Company Service Providers" in {
            val result = initial.tcspTypes(NewCompanyServiceProviders)
            result must be(Tcsp(tcspTypes = Some(NewCompanyServiceProviders)))
          }
        }
      }

    }
  }

}
