Axon MRI - Flow visualization tool
==================================

MRI helps you visualize flow of the code when using Axon Framework.
Upgrade spoon-core version.

Installation
------------

	gradle jar

Usage
-----

1. Before executing this tool you need to have maven project.

To print the Axon flow:

		java -jar build/libs/org.mri-VERSION.jar -m (--method-name) METHOD_NAME -s (--source-maven-project-folder) MAVEN_PROJECT

    -s (--source-folder) SOURCE_FOLDERS : source folder(s) for the analyzed maven project
		-m (--method-name) METHOD_NAME      : method name (can be a regexp) to print axon flow for
		-f (--format) [DEFAULT | PLANTUML]  : format of the output
		
Example
-------

Execute following from this project root directory:

	$ git clone https://github.com/hananbing/AxonBank.git ../AxonBank
	$ git checkout no-lombok
	$ java -jar build/libs/org.mri-*.jar -s ../AxonBank -m createTransfers -f plantuml

Output:

```
	@startuml create-transfers-flow.png
  participant "org.axonframework.samples.bank.api.banktransfer\n**CreateBankTransferCommand**" as CreateBankTransferCommand
  participant "org.axonframework.samples.bank.command\n**BankTransfer**" as BankTransfer
  participant "org.axonframework.samples.bank.api.banktransfer\n**BankTransferCreatedEvent**" as BankTransferCreatedEvent
  participant "org.axonframework.samples.bank.command\n**BankTransferManagementSaga**" as BankTransferManagementSaga
  participant "org.axonframework.samples.bank.api.bankaccount\n**DebitSourceBankAccountCommand**" as DebitSourceBankAccountCommand
  participant "org.axonframework.samples.bank.command\n**BankAccountCommandHandler**" as BankAccountCommandHandler
  participant "org.axonframework.samples.bank.command\n**BankAccount**" as BankAccount
  participant "org.axonframework.samples.bank.api.bankaccount\n**SourceBankAccountDebitedEvent**" as SourceBankAccountDebitedEvent
  participant "org.axonframework.samples.bank.api.bankaccount\n**CreditDestinationBankAccountCommand**" as CreditDestinationBankAccountCommand
  participant "org.axonframework.samples.bank.api.bankaccount\n**DestinationBankAccountCreditedEvent**" as DestinationBankAccountCreditedEvent
  participant "org.axonframework.samples.bank.api.banktransfer\n**MarkBankTransferCompletedCommand**" as MarkBankTransferCompletedCommand
  participant "org.axonframework.samples.bank.api.banktransfer\n**BankTransferCompletedEvent**" as BankTransferCompletedEvent
  participant "org.axonframework.samples.bank.query.banktransfer\n**BankTransferEventListener**" as BankTransferEventListener
  participant "org.axonframework.samples.bank.api.bankaccount\n**MoneyAddedEvent**" as MoneyAddedEvent
  participant "org.axonframework.samples.bank.query.bankaccount\n**BankAccountEventListener**" as BankAccountEventListener
  participant "org.axonframework.samples.bank.api.bankaccount\n**MoneySubtractedEvent**" as MoneySubtractedEvent
  participant "org.axonframework.samples.bank.api.bankaccount\n**SourceBankAccountDebitRejectedEvent**" as SourceBankAccountDebitRejectedEvent
  participant "org.axonframework.samples.bank.api.banktransfer\n**MarkBankTransferFailedCommand**" as MarkBankTransferFailedCommand
  participant "org.axonframework.samples.bank.api.banktransfer\n**BankTransferFailedEvent**" as BankTransferFailedEvent

  BankTransferController -> CreateBankTransferCommand: create
  CreateBankTransferCommand --> BankTransfer: <init>
  BankTransfer -> BankTransfer: create
  BankTransfer -> BankTransferCreatedEvent: <init>
  BankTransferCreatedEvent --> BankTransfer: on
  BankTransferCreatedEvent --> BankTransferManagementSaga: on
  BankTransferManagementSaga -> DebitSourceBankAccountCommand: create
  DebitSourceBankAccountCommand --> BankAccountCommandHandler: handle
  BankAccountCommandHandler -> BankAccount: create
  BankAccount -> SourceBankAccountDebitedEvent: <init>
  SourceBankAccountDebitedEvent --> BankTransferManagementSaga: on
  BankTransferManagementSaga -> CreditDestinationBankAccountCommand: create
  CreditDestinationBankAccountCommand --> BankAccountCommandHandler: handle
  BankAccount -> DestinationBankAccountCreditedEvent: <init>
  DestinationBankAccountCreditedEvent --> BankTransferManagementSaga: on
  BankTransferManagementSaga -> MarkBankTransferCompletedCommand: create
  MarkBankTransferCompletedCommand --> BankTransfer: handle
  BankTransfer -> BankTransferCompletedEvent: <init>
  BankTransferCompletedEvent --> BankTransfer: on
  BankTransferCompletedEvent --> BankTransferEventListener: on
  BankAccount -> MoneyAddedEvent: <init>
  MoneyAddedEvent --> BankAccount: on
  MoneyAddedEvent --> BankAccountEventListener: on
  BankAccount -> MoneySubtractedEvent: <init>
  MoneySubtractedEvent --> BankAccount: on
  MoneySubtractedEvent --> BankAccountEventListener: on
  BankAccount -> SourceBankAccountDebitRejectedEvent: <init>
  SourceBankAccountDebitRejectedEvent --> BankTransferManagementSaga: on
  BankTransferManagementSaga -> MarkBankTransferFailedCommand: create
  MarkBankTransferFailedCommand --> BankTransfer: handle
  BankTransfer -> BankTransferFailedEvent: <init>
  BankTransferFailedEvent --> BankTransfer: on
  BankTransferFailedEvent --> BankTransferEventListener: on
  BankTransferCreatedEvent --> BankTransferEventListener: on
  @enduml
```
	
Run (requires [plantuml](https://plantuml.com/zh/download) as shell command):

	$ java -jar build/libs/org.mri-*.jar -s ../AxonBank/ -m createTransfers -f plantuml | plantuml -tpng -pipe > output.png

Image output:

![Example Axon flow as Plant UML sequence diagram](./create-transfers-flow.png)

Contributors
-------

@pbadenski
@jweissman
@hananbing