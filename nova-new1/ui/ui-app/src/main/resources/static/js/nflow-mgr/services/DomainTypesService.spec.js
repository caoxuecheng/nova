define(["angular-mocks", "nflow-mgr/module-name", "nflow-mgr/module", "nflow-mgr/module-require"], function (mocks, moduleName) {
    describe("Service: DomainTypesService", function () {
        // Include dependencies
        beforeEach(mocks.module("nova", moduleName));

        // detectDomainType
        it("should detect domain type", mocks.inject(function (DomainTypesService) {
            var domainTypes = [{id: "0", regexPattern: "f|m"}];

            expect(DomainTypesService.detectDomainType("female", domainTypes)).toBe(null);
            expect(DomainTypesService.detectDomainType("f", domainTypes)).toBe(domainTypes[0]);
        }));
    });
});
