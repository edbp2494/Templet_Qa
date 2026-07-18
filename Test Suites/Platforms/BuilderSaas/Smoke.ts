<?xml version="1.0" encoding="UTF-8"?>
<TestSuiteEntity>
   <description>Smoke del Builder SaaS en una sola sesion de navegador: valida paginas server-side (deteccion de la regresion del middleware), sanity de /api/* con sesion, y proteccion 401 sin sesion.</description>
   <name>Smoke</name>
   <tag>NoRestart,BuilderSaas,Smoke,Middleware</tag>
   <isRerun>false</isRerun>
   <mailRecipient></mailRecipient>
   <numberOfRerun>0</numberOfRerun>
   <pageLoadTimeout>30</pageLoadTimeout>
   <pageLoadTimeoutDefault>true</pageLoadTimeoutDefault>
   <rerunFailedTestCasesOnly>false</rerunFailedTestCasesOnly>
   <rerunImmediately>false</rerunImmediately>
   <testSuiteGuid>5bd606b8-e184-4476-9ca2-dc3ecc068da3</testSuiteGuid>
   <testCaseLink>
      <guid>1986114e-76fa-46f6-a1de-b6b8c1ad718b</guid>
      <isReuseDriver>true</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/BuilderSaas/smoke/validate-smoke-pages</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>302fbf30-81db-4a00-a382-a14431c4b9d1</guid>
      <isReuseDriver>true</isReuseDriver>
      <isRun>false</isRun>
      <testCaseId>Test Cases/BuilderSaas/smoke/validate-api-sanity</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>13973b3d-0f15-4b0f-8fe1-69246c9cc98e</guid>
      <isReuseDriver>true</isReuseDriver>
      <isRun>false</isRun>
      <testCaseId>Test Cases/BuilderSaas/smoke/validate-auth-unauthorized</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
</TestSuiteEntity>
