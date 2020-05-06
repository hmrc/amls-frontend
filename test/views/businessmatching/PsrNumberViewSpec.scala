/*
 * Copyright 2020 HM Revenue & Customs
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

package views.businessmatching

import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.businessmatching.{BusinessAppliedForPSRNumber, BusinessAppliedForPSRNumberYes}
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture

class PsrNumberViewSpec extends AmlsViewSpec {

    trait ViewFixture extends Fixture {
        implicit val requestWithToken = addTokenForView()
    }

    "psr_number view" must {
        "have correct title for pre-submission mode" in new ViewFixture {

            val form2: ValidForm[BusinessAppliedForPSRNumber] = Form2(BusinessAppliedForPSRNumberYes("1234"))

            def view = views.html.businessmatching.psr_number(form2, edit = false, isPreSubmission = true)

            doc.title must startWith(Messages("businessmatching.psr.number.title") + " - " + Messages("summary.businessmatching"))
            heading.html must include(Messages("businessmatching.psr.number.title"))
            subHeading.html must include(Messages("summary.businessmatching"))

        }

        "have correct title for non-pre-submisson mode" in new ViewFixture {

            val form2: ValidForm[BusinessAppliedForPSRNumber] = Form2(BusinessAppliedForPSRNumberYes("1234"))

            def view = views.html.businessmatching.psr_number(form2, edit = true, isPreSubmission = false, isPsrDefined = true)

            doc.title must startWith(Messages("businessmatching.psr.number.title.post.submission") + " - " + Messages("summary.updateinformation"))
            heading.html must include(Messages("businessmatching.psr.number.title.post.submission"))
            subHeading.html must include(Messages("summary.updateinformation"))

        }

        "show errors in the correct locations" in new ViewFixture {

            val form2: InvalidForm = InvalidForm(Map.empty,
                Seq(
                    (Path \ "appliedFor") -> Seq(ValidationError("not a message Key")),
                    (Path \ "regNumber-panel") -> Seq(ValidationError("second not a message Key"))
                ))

            def view = views.html.businessmatching.psr_number(form2, edit = false)

            errorSummary.html() must include("not a message Key")
            errorSummary.html() must include("second not a message Key")

            doc.getElementById("appliedFor")
                    .getElementsByClass("error-notification").first().html() must include("not a message Key")

            doc.getElementById("regNumber-panel")
                    .getElementsByClass("error-notification").first().html() must include("second not a message Key")

        }

        "hide the return to progress link"in new ViewFixture {
            val form2: ValidForm[BusinessAppliedForPSRNumber] = Form2(BusinessAppliedForPSRNumberYes("1234"))

            def view = views.html.businessmatching.psr_number(form2, edit = true, showReturnLink = false)
            doc.body().text() must not include Messages("link.return.registration.progress")
        }

        "hide the Yes/No selection when editing an inputted PSR number and not in-presubmission mode" in new ViewFixture {
            val form2: ValidForm[BusinessAppliedForPSRNumber] = Form2(BusinessAppliedForPSRNumberYes("1234"))

            override def view = views.html.businessmatching.psr_number(form2, edit = true, isPreSubmission = false, isPsrDefined = true)

            doc.body().text() must not include "Yes"
            doc.body().text() must not include "No"
        }

        "hide the Yes/No selection when editing an inputted PSR number and not in-presubmission mode and not in edit mode" in new ViewFixture {
            val form2: ValidForm[BusinessAppliedForPSRNumber] = Form2(BusinessAppliedForPSRNumberYes("1234"))

            override def view = views.html.businessmatching.psr_number(form2, edit = false, isPreSubmission = false, isPsrDefined = true)

            doc.body().text() must not include "Yes"
            doc.body().text() must not include "No"
        }

        "show the Yes/No selection when editing an inputted PSR number and in pre-submission mode" in new ViewFixture {
            override def view = views.html.businessmatching.psr_number(EmptyForm, edit = true, isPreSubmission = true)

            doc.body().text() must include("Yes")
            doc.body().text() must include("No")
        }

        "show the Yes/No selection when not editing" in new ViewFixture {
            override def view = views.html.businessmatching.psr_number(EmptyForm, edit = false)

            doc.body().text() must include("Yes")
            doc.body().text() must include("No")
        }

        "have a back link in pre-submission mode" in new ViewFixture {
            def view = views.html.businessmatching.psr_number(EmptyForm, edit = false, isPreSubmission = true)

            doc.getElementsByAttributeValue("class", "link-back") must not be empty
        }

        "have a back link in non pre-submission mode" in new ViewFixture {
            def view = views.html.businessmatching.psr_number(EmptyForm, edit = true, isPreSubmission = false)

            doc.getElementsByAttributeValue("class", "link-back") must not be empty
        }
    }
}