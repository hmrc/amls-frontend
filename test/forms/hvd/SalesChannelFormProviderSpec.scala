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

package forms.hvd

import forms.behaviours.CheckboxFieldBehaviours
import models.hvd.{HowWillYouSellGoods, SalesChannel}
import play.api.data.FormError

class SalesChannelFormProviderSpec extends CheckboxFieldBehaviours {

  val form = new SalesChannelFormProvider()()

  "form" must {

    val fieldName   = "salesChannels"
    val requiredKey = "error.required.hvd.how-will-you-sell-goods"

    behave like checkboxFieldWithWrapper[SalesChannel, HowWillYouSellGoods](
      form,
      fieldName,
      validValues = SalesChannel.all,
      x => HowWillYouSellGoods(Set(x)),
      x => HowWillYouSellGoods(x.toSet),
      invalidError = FormError(s"$fieldName[0]", requiredKey)
    )

    behave like mandatoryCheckboxField(
      form,
      fieldName,
      requiredKey
    )
  }

}
