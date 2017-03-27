package models.notifications

import models.notifications.ContactType.RejectionReasons
import org.scalatestplus.play.PlaySpec

class ContactTypeSpec  extends PlaySpec{

  "ContactType" must {
    "be created correctly from path parameter" in {


      ContactType.pathBinder.bind("thing",RejectionReasons.toString) mustBe(Right(RejectionReasons))

    }
  }

}
