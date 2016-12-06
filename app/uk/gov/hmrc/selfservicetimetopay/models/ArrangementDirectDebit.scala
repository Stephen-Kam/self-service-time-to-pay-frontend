/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.selfservicetimetopay.models

case class ArrangementDirectDebit(accountName:String,
                                  sortCode1:Int,
                                  sortCode2:Int,
                                  sortCode3:Int,
                                  accountNumber:Long,
                                  confirmed: Option[Boolean]) {

  def sortCode: String = Seq(sortCode1, sortCode2, sortCode3).mkString
}

object ArrangementDirectDebit {
  def from(bankDetails: BankDetails): ArrangementDirectDebit = {
    ArrangementDirectDebit(bankDetails.accountName.getOrElse(""),
      bankDetails.sortCode.substring(0, 2).toInt,
      bankDetails.sortCode.substring(2, 4).toInt,
      bankDetails.sortCode.substring(4, 6).toInt,
      bankDetails.accountNumber.toLong,
      None)
  }
}