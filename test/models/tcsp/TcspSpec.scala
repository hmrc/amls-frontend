package models.tcsp

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json


class TcspSpec extends PlaySpec with MockitoSugar {

  "Tcsp" must {

    val CompanyServiceProviders = TrustOrCompanyServiceProviders(Set(NomineeShareholdersProvider, TrusteeProvider, CompanyDirectorEtc(true, false)))

    "correctly convert between json formats" must {

      val completeJson = Json.obj(
        "serviceProviders[]" -> Seq("01", "02" ,"04"),
        "onlyOffTheShelfCompsSold" -> Seq("true"),
        "complexCorpStructureCreation" -> Seq("false")
      )
      val completeModel = Tcsp(Some(CompanyServiceProviders))

      "Serialise as expected" ignore {
        Json.toJson(completeModel) must be(completeJson)
      }

      "Deserialise as expected" ignore {
        completeJson.as[Tcsp] must be(completeModel)
      }

    }

  }

}
