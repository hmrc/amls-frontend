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

import forms.mappings.Mappings
import models.hvd.{HowWillYouSellGoods, SalesChannel}
import play.api.data.Form
import play.api.data.Forms.seq

import javax.inject.Inject

class SalesChannelFormProvider @Inject() () extends Mappings {

  private val errorMessage               = "error.required.hvd.how-will-you-sell-goods"
  def apply(): Form[HowWillYouSellGoods] = Form[HowWillYouSellGoods](
    "salesChannels" -> seq(enumerable[SalesChannel](errorMessage, errorMessage)(SalesChannel.enumerable))
      .verifying(nonEmptySeq(errorMessage))
      .transform[HowWillYouSellGoods](channels => HowWillYouSellGoods(channels.toSet), _.channels.toSeq)
  )
}
