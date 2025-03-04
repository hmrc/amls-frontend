/*
 * Copyright 2024 HM Revenue & Customs
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

package utils.businessdetails

import models.businessdetails._
import models.{Country, DateOfChange}
import org.scalatest.Assertion
import uk.gov.hmrc.govukfrontend.views.Aliases.{SummaryListRow, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Actions}
import utils.{AmlsSpec, CheckYourAnswersHelperFunctions, DateHelper}

import java.time.LocalDate

class CheckYourAnswersHelperSpec extends AmlsSpec with CheckYourAnswersHelperFunctions {

  lazy val cyaHelper: CheckYourAnswersHelper = app.injector.instanceOf[CheckYourAnswersHelper]

  val now       = LocalDate.now()
  val startDate = now.minusYears(5)
  val vatNo     = "946185"
  val phoneNo   = "07174625489"
  val email     = "test@email.com"

  val model = BusinessDetails(
    Some(PreviouslyRegisteredYes(Some("19462729"))),
    Some(ActivityStartDate(startDate)),
    Some(VATRegisteredYes(vatNo)),
    Some(CorporationTaxRegisteredYes("FJIEI284791862S")),
    Some(ContactingYou(Some(phoneNo), Some(email))),
    Some(RegisteredOfficeIsUK(true)),
    Some(
      RegisteredOfficeUK(
        "Line 1",
        Some("Line 2"),
        Some("Line 3"),
        Some("Line 4"),
        "AG1 3RE",
        Some(DateOfChange(now.minusYears(1)))
      )
    ),
    Some(true),
    Some(CorrespondenceAddressIsUk(true)),
    Some(
      CorrespondenceAddress(
        Some(
          CorrespondenceAddressUk(
            "John Smith",
            "Big Corp",
            "Line A",
            Some("Line B"),
            Some("Line C"),
            Some("Line D"),
            "UE3 5DQ"
          )
        ),
        None
      )
    )
  )

  trait RowFixture {

    val summaryListRows: Seq[SummaryListRow]

    def assertRowMatches(index: Int, title: String, value: String, changeUrl: String, changeId: String): Assertion = {

      val result = summaryListRows.lift(index).getOrElse(fail(s"Row for index $index does not exist"))

      result.key.toString must include(messages(title))

      result.value.toString must include(value)

      checkChangeLink(result, changeUrl, changeId)
    }

    def checkChangeLink(slr: SummaryListRow, href: String, id: String): Assertion = {
      val changeLink = slr.actions.flatMap(_.items.headOption).getOrElse(fail("No edit link present"))

      changeLink.content.toString must include(messages("button.edit"))
      changeLink.href mustBe href
      changeLink.attributes("id") mustBe id
    }
  }

  ".createSummaryList" when {

    "Previously Registered is present" must {

      "display the correct row" when {

        "answer is true and showRegisteredForMLR is true" in new RowFixture {

          override val summaryListRows: Seq[SummaryListRow] =
            cyaHelper.createSummaryList(model, true).rows

          assertRowMatches(
            0,
            "businessdetails.registeredformlr.title",
            booleanToLabel(true),
            controllers.businessdetails.routes.PreviouslyRegisteredController.get(true).url,
            "businessdetailsregform-edit"
          )
        }

        "answer is false and showRegisteredForMLR is true" in new RowFixture {

          override val summaryListRows: Seq[SummaryListRow] =
            cyaHelper
              .createSummaryList(
                model.copy(previouslyRegistered = Some(PreviouslyRegisteredNo)),
                true
              )
              .rows

          assertRowMatches(
            0,
            "businessdetails.registeredformlr.title",
            booleanToLabel(false),
            controllers.businessdetails.routes.PreviouslyRegisteredController.get(true).url,
            "businessdetailsregform-edit"
          )
        }
      }

      "not display the row when showRegisteredForMLR is false" in new RowFixture {

        override val summaryListRows: Seq[SummaryListRow] =
          cyaHelper.createSummaryList(model, false).rows

        summaryListRows.head mustNot be(
          row(
            "businessdetails.registeredformlr.title",
            booleanToLabel(true),
            Some(
              Actions(
                items = Seq(
                  ActionItem(
                    controllers.businessdetails.routes.PreviouslyRegisteredController.get(true).url,
                    Text(messages("button.edit")),
                    attributes = Map("id" -> "businessdetailsregform-edit")
                  )
                )
              )
            )
          )
        )
      }
    }

    "Activity Start Date is present" must {

      "render the correct row" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] =
          cyaHelper.createSummaryList(model, true).rows

        assertRowMatches(
          1,
          "businessdetails.activity.start.date.title",
          DateHelper.formatDate(startDate),
          controllers.businessdetails.routes.ActivityStartDateController.get(true).url,
          "businessdetailsactivitystartdate-edit"
        )
      }
    }

    "VAT Registered is present" must {

      "render the correct rows for yes" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] =
          cyaHelper.createSummaryList(model, true).rows

        assertRowMatches(
          2,
          "businessdetails.registeredforvat.title",
          booleanToLabel(true),
          controllers.businessdetails.routes.VATRegisteredController.get(true).url,
          "businessdetailsregformvat-edit"
        )

        val vatNoRow = summaryListRows.lift(3).map(x => (x.key.toString, x.value.toString))

        vatNoRow.value._1 must include(messages("lbl.vat.reg.number"))

        vatNoRow.value._2 must include(vatNo)
      }

      "render the correct row for no" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] =
          cyaHelper.createSummaryList(model.copy(vatRegistered = Some(VATRegisteredNo)), true).rows

        assertRowMatches(
          2,
          "businessdetails.registeredforvat.title",
          booleanToLabel(false),
          controllers.businessdetails.routes.VATRegisteredController.get(true).url,
          "businessdetailsregformvat-edit"
        )
      }
    }

    "Registered Office is present" must {

      "render the correct rows" when {

        "Office is in UK" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] =
            cyaHelper.createSummaryList(model, true).rows

          assertRowMatches(
            4,
            "businessdetails.registeredoffice.title",
            booleanToLabel(true),
            controllers.businessdetails.routes.RegisteredOfficeIsUKController.get(true).url,
            "businessdetailsregisteredofficeisuk-edit"
          )

          assertRowMatches(
            5,
            "businessdetails.registeredoffice.where.title",
            addressToLines(model.registeredOffice.value.toLines).content.toString,
            controllers.businessdetails.routes.RegisteredOfficeUKController.get(true).url,
            "businessdetailsregoffice-edit"
          )
        }

        "office is outside the UK" in new RowFixture {

          val address = Seq("line1", "line2", "line3", "line4", "United States")

          override val summaryListRows: Seq[SummaryListRow] =
            cyaHelper
              .createSummaryList(
                model.copy(registeredOffice =
                  Some(
                    RegisteredOfficeNonUK(
                      address(0),
                      Some(address(1)),
                      Some(address(2)),
                      Some(address(3)),
                      Country(address(4), "US"),
                      None
                    )
                  )
                ),
                true
              )
              .rows

          assertRowMatches(
            4,
            "businessdetails.registeredoffice.title",
            booleanToLabel(false),
            controllers.businessdetails.routes.RegisteredOfficeIsUKController.get(true).url,
            "businessdetailsregisteredofficeisuk-edit"
          )

          assertRowMatches(
            5,
            "businessdetails.registeredoffice.where.title",
            addressToLines(address).content.toString,
            controllers.businessdetails.routes.RegisteredOfficeNonUKController.get(true).url,
            "businessdetailsregoffice-edit"
          )
        }
      }
    }

    "Contacting You is present" must {

      "render the correct rows" in new RowFixture {

        override val summaryListRows: Seq[SummaryListRow] =
          cyaHelper.createSummaryList(model, true).rows

        assertRowMatches(
          6,
          "businessdetails.contactingyou.email.title",
          email,
          controllers.businessdetails.routes.BusinessEmailAddressController.get(true).url,
          "businessdetailscontactyou-edit"
        )

        assertRowMatches(
          7,
          "businessdetails.contactingyou.phone.title",
          phoneNo,
          controllers.businessdetails.routes.ContactingYouPhoneController.get(true).url,
          "businessdetailscontactphone-edit"
        )
      }
    }

    "Correspondence Address is present" must {

      "render the correct rows" when {

        "answer is no" in new RowFixture {

          override val summaryListRows: Seq[SummaryListRow] =
            cyaHelper.createSummaryList(model.copy(altCorrespondenceAddress = Some(false)), true).rows

          assertRowMatches(
            8,
            "businessdetails.lettersaddress.title",
            booleanToLabel(true),
            controllers.businessdetails.routes.LettersAddressController.get(true).url,
            "businessdetailslettersaddress-edit"
          )
        }

        "answer is yes and address is in the UK" in new RowFixture {

          override val summaryListRows: Seq[SummaryListRow] =
            cyaHelper.createSummaryList(model, true).rows

          assertRowMatches(
            8,
            "businessdetails.lettersaddress.title",
            booleanToLabel(false),
            controllers.businessdetails.routes.LettersAddressController.get(true).url,
            "businessdetailslettersaddress-edit"
          )

          assertRowMatches(
            9,
            "businessdetails.correspondenceaddress.isuk.title",
            messages("businessdetails.correspondenceaddress.ukAddress"),
            controllers.businessdetails.routes.CorrespondenceAddressIsUkController.get(true).url,
            "businessdetailscorraddressisuk-edit"
          )

          assertRowMatches(
            10,
            "businessdetails.correspondenceaddress.title",
            addressToLines(model.correspondenceAddress.flatMap(_.ukAddress).value.toLines).content.toString,
            controllers.businessdetails.routes.CorrespondenceAddressUkController.get(true).url,
            "businessdetailscorraddress-edit"
          )
        }

        "answer is yes and address is not in the UK" in new RowFixture {

          val nonUkAddress = CorrespondenceAddressNonUk(
            "Ben Jones",
            "Business Ltd",
            "123 Street",
            Some("Test Lane"),
            None,
            None,
            Country("United States", "US")
          )

          override val summaryListRows: Seq[SummaryListRow] =
            cyaHelper
              .createSummaryList(
                model.copy(correspondenceAddress = Some(CorrespondenceAddress(None, Some(nonUkAddress)))),
                true
              )
              .rows

          assertRowMatches(
            8,
            "businessdetails.lettersaddress.title",
            booleanToLabel(false),
            controllers.businessdetails.routes.LettersAddressController.get(true).url,
            "businessdetailslettersaddress-edit"
          )

          assertRowMatches(
            9,
            "businessdetails.correspondenceaddress.isuk.title",
            messages("businessdetails.correspondenceaddress.nonUkAddress"),
            controllers.businessdetails.routes.CorrespondenceAddressIsUkController.get(true).url,
            "businessdetailscorraddressisuk-edit"
          )

          assertRowMatches(
            10,
            "businessdetails.correspondenceaddress.title",
            addressToLines(nonUkAddress.toLines).content.toString,
            controllers.businessdetails.routes.CorrespondenceAddressNonUkController.get(true).url,
            "businessdetailscorraddress-edit"
          )
        }
      }
    }
  }
}
