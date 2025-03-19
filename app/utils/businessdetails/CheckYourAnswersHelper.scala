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

import config.ApplicationConfig
import models.businessdetails._
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.Aliases._
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Actions, SummaryList}
import utils.DateHelper
import views.html.components.{Button => SubmissionButton}

import javax.inject.Inject

class CheckYourAnswersHelper @Inject() (button: SubmissionButton, appConfig: ApplicationConfig) {

  def createSummaryList(businessDetails: BusinessDetails, showRegisteredForMLR: Boolean)(implicit
    messages: Messages
  ): SummaryList = {

    val rows: Seq[SummaryListRow] = Seq(
      businessCurrentlyRegisteredRow(showRegisteredForMLR, businessDetails),
      activityStartDateRow(businessDetails)
    ).flatten ++
      vatRegisteredRow(businessDetails).getOrElse(Nil) ++
      registeredOfficeRow(businessDetails).getOrElse(Nil) ++
      contactingYouRows(businessDetails).getOrElse(Nil) ++
      correspondenceAddressRows(businessDetails).getOrElse(Nil)

    SummaryList(rows)
  }

  private def businessCurrentlyRegisteredRow(showRegisteredForMLR: Boolean, businessMatching: BusinessDetails)(implicit
    messages: Messages
  ): Option[SummaryListRow] = {

    val labelOpt = businessMatching.previouslyRegistered.map {
      case PreviouslyRegisteredNo     => messages("lbl.no")
      case PreviouslyRegisteredYes(_) => messages("lbl.yes")
    }

    if (showRegisteredForMLR) {
      labelOpt map { label =>
        row(
          "businessdetails.registeredformlr.title",
          label,
          Some(
            Actions(
              items = Seq(
                ActionItem(
                  controllers.businessdetails.routes.PreviouslyRegisteredController.get(true).url,
                  Text(messages("button.edit")),
                  visuallyHiddenText = Some(messages("businessactivities.checkYourAnswers.change.descOthrActs")),
                  attributes = Map("id" -> "businessdetailsregform-edit")
                )
              )
            )
          )
        )
      }
    } else None
  }

  private def activityStartDateRow(
    businessDetails: BusinessDetails
  )(implicit messages: Messages): Option[SummaryListRow] =
    businessDetails.activityStartDate map { activityStartDate =>
      row(
        "businessdetails.activity.start.date.cya.lbl",
        DateHelper.formatDate(activityStartDate.startDate),
        Some(
          Actions(
            items = Seq(
              ActionItem(
                controllers.businessdetails.routes.ActivityStartDateController.get(true).url,
                Text(messages("button.edit")),
                visuallyHiddenText = Some(messages("businessdetails.checkYourAnswers.change.whenActvsStartd")),
                attributes = Map("id" -> "businessdetailsactivitystartdate-edit")
              )
            )
          )
        )
      )
    }

  private def vatRegisteredRow(
    businessDetails: BusinessDetails
  )(implicit messages: Messages): Option[Seq[SummaryListRow]] = {

    val vatAction = editAction(
      controllers.businessdetails.routes.VATRegisteredController.get(true).url,
      "businessdetails.checkYourAnswers.change.busRegtdVAT",
      "businessdetailsregformvat-edit"
    )

    businessDetails.vatRegistered.map {
      case VATRegisteredNo         => Seq(row("businessdetails.registeredforvat.title", messages("lbl.no"), vatAction))
      case VATRegisteredYes(vatNo) =>
        Seq(
          row("businessdetails.registeredforvat.title", messages("lbl.yes"), vatAction),
          row("lbl.vat.reg.number", vatNo, None)
        )
    }
  }

  private def registeredOfficeRow(
    businessDetails: BusinessDetails
  )(implicit messages: Messages): Option[Seq[SummaryListRow]] = {

    val yesNoEdit = editAction(
      controllers.businessdetails.routes.RegisteredOfficeIsUKController.get(true).url,
      "businessdetails.checkYourAnswers.change.officeInUK",
      "businessdetailsregisteredofficeisuk-edit"
    )

    businessDetails.registeredOffice.map {
      case ukAddress @ RegisteredOfficeUK(_, _, _, _, _, _) =>
        Seq(
          row(
            "businessdetails.registeredoffice.title",
            messages("lbl.yes"),
            yesNoEdit
          ),
          SummaryListRow(
            Key(
              Text(messages("businessdetails.registeredoffice.where.cya.lbl"))
            ),
            addressToLines(ukAddress.toLines),
            actions = editAction(
              controllers.businessdetails.routes.RegisteredOfficeUKController.get(true).url,
              "businessdetails.checkYourAnswers.change.officeRegstd",
              "businessdetailsregoffice-edit"
            )
          )
        )

      case nonUkAddress @ RegisteredOfficeNonUK(_, _, _, _, _, _) =>
        Seq(
          row(
            "businessdetails.registeredoffice.title",
            messages("lbl.no"),
            yesNoEdit
          ),
          SummaryListRow(
            Key(
              Text(messages("businessdetails.registeredoffice.where.cya.lbl"))
            ),
            addressToLines(nonUkAddress.toLines),
            actions = editAction(
              controllers.businessdetails.routes.RegisteredOfficeNonUKController.get(true).url,
              "businessdetails.checkYourAnswers.change.addrUkBased",
              "businessdetailsregoffice-edit"
            )
          )
        )
    }
  }

  private def contactingYouRows(
    businessDetails: BusinessDetails
  )(implicit messages: Messages): Option[Seq[SummaryListRow]] =
    for {
      contactingYou <- businessDetails.contactingYou
      email         <- contactingYou.email
      phoneNumber   <- contactingYou.phoneNumber
    } yield Seq(
      row(
        "businessdetails.contactingyou.email.cya.lbl",
        email,
        editAction(
          controllers.businessdetails.routes.BusinessEmailAddressController.get(true).url,
          "businessdetails.checkYourAnswers.change.busEmail",
          "businessdetailscontactyou-edit"
        )
      ),
      row(
        "businessdetails.contactingyou.phone.cya.lbl",
        phoneNumber,
        editAction(
          controllers.businessdetails.routes.ContactingYouPhoneController.get(true).url,
          "businessdetails.checkYourAnswers.change.busTelNo",
          "businessdetailscontactphone-edit"
        )
      )
    )

  private def correspondenceAddressRows(
    businessDetails: BusinessDetails
  )(implicit messages: Messages): Option[Seq[SummaryListRow]] = {

    def lettersAddressRow(label: String) = row(
      "businessdetails.lettersaddress.title",
      messages(label),
      editAction(
        controllers.businessdetails.routes.LettersAddressController.get(true).url,
        "businessdetails.checkYourAnswers.change.addrSame",
        "businessdetailslettersaddress-edit"
      )
    )

    def addressIsUKRow(label: String) = row(
      "businessdetails.correspondenceaddress.isuk.title",
      messages(label),
      editAction(
        controllers.businessdetails.routes.CorrespondenceAddressIsUkController.get(true).url,
        "businessdetails.checkYourAnswers.change.addrUkBased",
        "businessdetailscorraddressisuk-edit"
      )
    )

    (businessDetails.altCorrespondenceAddress, businessDetails.correspondenceAddress) match {
      case (Some(false), _)                                                                   => Some(Seq(lettersAddressRow("lbl.yes")))
      case (Some(true), Some(address: CorrespondenceAddress)) if address.isUk.contains(true)  =>
        address.ukAddress.map { address =>
          Seq(
            lettersAddressRow("lbl.no"),
            addressIsUKRow("businessdetails.correspondenceaddress.ukAddress"),
            SummaryListRow(
              Key(
                Text(messages("businessdetails.correspondenceaddress.cya.lbl"))
              ),
              addressToLines(address.toLines),
              actions = editAction(
                controllers.businessdetails.routes.CorrespondenceAddressUkController.get(true).url,
                "businessdetails.checkYourAnswers.change.addrForLettrs",
                "businessdetailscorraddress-edit"
              )
            )
          )
        }
      case (Some(true), Some(address: CorrespondenceAddress)) if address.isUk.contains(false) =>
        address.nonUkAddress.map { address =>
          Seq(
            lettersAddressRow("lbl.no"),
            addressIsUKRow("businessdetails.correspondenceaddress.nonUkAddress"),
            SummaryListRow(
              Key(
                Text(messages("businessdetails.correspondenceaddress.cya.lbl"))
              ),
              addressToLines(address.toLines),
              actions = editAction(
                controllers.businessdetails.routes.CorrespondenceAddressNonUkController.get(true).url,
                "businessdetails.checkYourAnswers.change.addrForLettrs",
                "businessdetailscorraddress-edit"
              )
            )
          )
        }
      case (_, _)                                                                             => None
    }
  }
  private def row(title: String, label: String, actions: Option[Actions])(implicit messages: Messages) =
    SummaryListRow(
      Key(Text(messages(title))),
      Value(Text(label)),
      actions = actions
    )

  private def editAction(route: String, hiddenText: String, id: String)(implicit messages: Messages) =
    Some(
      Actions(
        items = Seq(
          ActionItem(
            route,
            Text(messages("button.edit")),
            visuallyHiddenText = Some(messages(hiddenText)),
            attributes = Map("id" -> id)
          )
        )
      )
    )
  private def addressToLines(addressLines: Seq[String]): Value                                       = Value(
    HtmlContent(
      Html(
        "<ul class=\"govuk-list\">" +
          addressLines.map { line =>
            s"""<li>$line<li>"""
          }.mkString
          + "</ul>"
      )
    )
  )
}
