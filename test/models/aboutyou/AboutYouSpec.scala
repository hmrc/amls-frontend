package models.aboutyou

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class AboutYouSpec extends PlaySpec with MockitoSugar {

  val yourDetails = YourDetails(
    "firstname", None, "lastname"
  )

//  val role = RoleWithinBusiness(
//    "01",
//    ""
//  )

  "AboutYou" must {
    val completeJson = Json.obj(
      "firstName" -> "firstname",
      "lastName" -> "lastname",
      "roleWithinBusiness" -> "01",
      "other" -> ""
    )

    val completeModel = AboutYou(Some(yourDetails), None)

    "Serialise as expected" in {

      Json.toJson(completeModel) must
        be(completeJson)
    }

    "Deserialise as expected" in {

      completeJson.as[AboutYou] must
        be(completeModel)
    }
  }

  "Partially complete AboutYou" must {

    val partialJson = Json.obj(
      "firstName" -> "firstname",
      "lastName" -> "lastname"
    )

    val partialModel = AboutYou(Some(yourDetails), None)

    "Serialise as expected" in {

      Json.toJson(partialModel) must
        be(partialJson)
    }

    "Deserialise as expected" in {

      partialJson.as[AboutYou] must
        be(partialModel)
    }
  }

  "None" when {
    val initial : Option[AboutYou] = None

    "Merged with your details" must {
      "return AboutYou with correct details set" in {
        val result = initial.yourDetails(yourDetails)
        result must be (AboutYou(Some(yourDetails), None))
      }
    }

//    "Merged with yourRoleIntheBusinsess" must {
//      "return AboutYou with correct role in the business set" in {
//        val result = initial.roleWithinBusiness(role)
//        result must be (AboutYou(None, Some(role)))
//      }
//    }
  }

  "AboutYou" when {
    "yourDetails already set" when {
      val initial = AboutYou(Some(yourDetails), None)

      "Merged with your details" must {
        "return AboutYou with correct details set" in {
          val newDetails = YourDetails("TestName2", Some("TestName3"), "TestName4")
          val result = initial.yourDetails(newDetails)
          result must be (AboutYou(Some(newDetails), None))
        }
      }

//      "Merged with yourRoleIntheBusinsess" must {
//        "return AboutYou with correct role in the business set" in {
//          val newRole = RoleWithinBusiness("TestRoleWithinTheBusiness2", "")
//          val result = initial.roleWithinBusiness(newRole)
//          result must be (AboutYou(Some(yourDetails), Some(newRole)))
//        }
//      }
    }
  }

  "AboutYou" when {
//    "yourRoleInTheBusiness already set" when {
//      val initial = AboutYou(None, Some(role))
//      "Merged with your details" must {
//        "return AboutYou with correct details set" in {
//          val newDetails = YourDetails("TestName2", Some("TestName3"), "TestName4")
//          val result = initial.yourDetails(newDetails)
//          result must be (AboutYou(Some(newDetails), Some(role)))
//        }
//      }

//      "Merged with yourRoleIntheBusinsess" must {
//        "return AboutYou with correct role in the business set" in {
//          val newRole = RoleWithinBusiness("TestRoleWithinTheBusiness2", "")
//          val result = initial.roleWithinBusiness(newRole)
//          result must be (AboutYou(None, Some(newRole)))
//        }
//      }
//    }
  }
}