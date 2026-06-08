<?xml version="1.0" encoding="UTF-8"?>
<TestSuiteEntity>
   <description>Suite full cross-platform: valida y captura objetos visibles por plataforma, luego ejecuta flujos clave (list-view, list-actions, sidebar traversal, tracking tabs). Un bloque por plataforma con isReuseDriver para evitar logins repetidos.</description>
   <name>Full-Validation-All-Platforms</name>
   <tag>Full,CrossPlatform,Objects,Capture</tag>
   <isRerun>false</isRerun>
   <mailRecipient></mailRecipient>
   <numberOfRerun>0</numberOfRerun>
   <pageLoadTimeout>30</pageLoadTimeout>
   <pageLoadTimeoutDefault>true</pageLoadTimeoutDefault>
   <rerunFailedTestCasesOnly>false</rerunFailedTestCasesOnly>
   <rerunImmediately>false</rerunImmediately>

   <!-- ===== SHEETS ===== -->
   <testCaseLink>
      <guid>full-sheets-01</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Sheets/objects/validate-objects-visible</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>full-sheets-02</guid>
      <isReuseDriver>true</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Sheets/objects/list-view-visible-clickable</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>full-sheets-03</guid>
      <isReuseDriver>true</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Sheets/objects/list-actions-click-response</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>

   <!-- ===== DECKS ===== -->
   <testCaseLink>
      <guid>full-decks-01</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Decks/objects/validate-objects-visible</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>full-decks-02</guid>
      <isReuseDriver>true</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Decks/objects/list-view-visible-clickable</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>full-decks-03</guid>
      <isReuseDriver>true</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Decks/objects/list-actions-click-response</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>

   <!-- ===== EMAIL ===== -->
   <testCaseLink>
      <guid>full-email-01</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Email/objects/validate-objects-visible</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>full-email-02</guid>
      <isReuseDriver>true</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Email/objects/list-view-visible-clickable</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>full-email-03</guid>
      <isReuseDriver>true</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Email/objects/list-actions-click-response</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>

   <!-- ===== BUILDERS - sidebar ===== -->
   <testCaseLink>
      <guid>full-builders-01</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Builders/objects/validate-objects-visible</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>full-builders-02</guid>
      <isReuseDriver>true</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Builders/objects/click-visible-objects-return-home</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>

   <!-- ===== BUILDERS - tracking (bloque independiente, propio login) ===== -->
   <testCaseLink>
      <guid>full-tracking-01</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Builders/tracking/validate-all-dashboard</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>full-tracking-02</guid>
      <isReuseDriver>true</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Builders/tracking/validate-blueprint-tab</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>full-tracking-03</guid>
      <isReuseDriver>true</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Builders/tracking/validate-task-creation-tab</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>full-tracking-04</guid>
      <isReuseDriver>true</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Builders/tracking/validate-login-tab</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>

   <!-- ===== SCHEDULERS ===== -->
   <testCaseLink>
      <guid>full-schedulers-01</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Schedulers/objects/validate-objects-visible</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>full-schedulers-02</guid>
      <isReuseDriver>true</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Schedulers/objects/click-visible-objects-return-home</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>

</TestSuiteEntity>
