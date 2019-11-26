/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package views.businessmatching.updateservice.add

import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.businessmatching.{AccountancyServices, ArtMarketParticipant, BillPaymentServices, BusinessActivities, EstateAgentBusinessService, HighValueDealing, MoneyServiceBusiness, TelephonePaymentService, TrustAndCompanyServices}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsSpec
import views.Fixture
import views.html.businessmatching.updateservice.add._

class select_activitiesSpec extends AmlsSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    override def view = select_activities(EmptyForm,
      edit = true,
      Seq.empty[String],
      Seq.empty[String]
    )
  }

  "The select_Activities view" must {

    "have the correct title" in new ViewFixture {
      doc.title must startWith(Messages("businessmatching.updateservice.selectactivities.title") + " - " + Messages("summary.updateservice"))
    }

    "have correct heading" in new ViewFixture {
      heading.html must be(Messages("businessmatching.updateservice.selectactivities.heading"))
    }

    "have correct subHeading" in new ViewFixture {
      subHeading.html must include(Messages("summary.updateservice"))
    }

    "have the back link button" in new ViewFixture {
      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }

    "show the correct content" in new ViewFixture {

      val addedActivities = Seq(AccountancyServices, BillPaymentServices)
      val submittedActivities = Seq(MoneyServiceBusiness)

      override def view = select_activities(EmptyForm,
        edit = true,
        addedActivities map BusinessActivities.getValue,
        submittedActivities map (_.getMessage())
      )

      doc.body().text() must not include Messages("link.return.registration.progress")

      addedActivities foreach { a =>
        doc.body().text must include(Messages(a.getMessage()))
        doc.body().html() must include(BusinessActivities.getValue(a))
      }
    }

    "not show the return link" in new ViewFixture {
      override def view = select_activities(EmptyForm,
        edit = true,
        Seq.empty[String],
        Seq.empty[String]
      )

      doc.body().text() must not include Messages("link.return.registration.progress")
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq((Path \ "businessmatching.updateservice.selectactivities") -> Seq(ValidationError("not a message Key"))))

      override def view = select_activities(form2, edit = true, Seq.empty[String], Seq.empty[String])

      errorSummary.html() must include("not a message Key")
    }

    "have correct hint content" in new ViewFixture {
      val form2: ValidForm[BusinessActivities] = Form2(BusinessActivities(Set(
        AccountancyServices,
        MoneyServiceBusiness,
        TrustAndCompanyServices,
        TelephonePaymentService,
        ArtMarketParticipant,
        BillPaymentServices,
        EstateAgentBusinessService,
        HighValueDealing)))

      override def view = select_activities(form2, edit = true, Seq("01", "02", "03", "04", "05", "06", "07", "08"), Seq.empty[String])

      doc.html must include("They provide services like professional bookkeeping, accounts preparation and signing, and tax advice.")
      doc.html must include("They facilitate and engage in the selling of art for €10,000 or more. Roles include things like art agents, art auctioneers, art dealers, and gallery owners.")
      doc.html must include("They handle payments for utility and other household bills on behalf of customers.")
      doc.html must include("They introduce and act on instructions from people who want to buy or sell property. They also secure the purchase or sale of property.")
      doc.html must include("They accept or make cash payments of €10,000 or more (or equivalent) in exchange for goods. This includes when a customer deposits cash directly into a bank account. Estate agents are not classed as high value dealers.")
      doc.html must include("They exchange currency, transmit money, or cash cheques for their customers.")
      doc.html must include("They form companies, and supply services like providing a business or correspondence address. They also act, or arrange for another person to act, in a certain role. For example, a director in a company or a trustee of an express trust.")
      doc.html must include("They act as a link between customers and suppliers. They handle payments made through devices like mobile phones, computers, and smart TVs.")
    }
  }
}