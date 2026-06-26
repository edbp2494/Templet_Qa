<?xml version="1.0" encoding="UTF-8"?>
<TestSuiteEntity>
   <description>Re-corre SOLO el caso Schedulers Requests, aislado, para confirmar si el fallo es real o fue por el cierre de ventana del navegador (Connection reset / target window closed) que ocurrio en la corrida del 25-jun.</description>
   <name>Revalidate-Requests-Only</name>
   <tag>QA,Revalidacion,Schedulers</tag>
   <isRerun>true</isRerun>
   <mailRecipient></mailRecipient>
   <numberOfRerun>1</numberOfRerun>
   <pageLoadTimeout>30</pageLoadTimeout>
   <pageLoadTimeoutDefault>true</pageLoadTimeoutDefault>
   <rerunFailedTestCasesOnly>false</rerunFailedTestCasesOnly>
   <rerunImmediately>true</rerunImmediately>
   <testCaseLink>
      <guid>revalidate-requests-0001</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Schedulers/requests/validate-requests-list</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
</TestSuiteEntity>
