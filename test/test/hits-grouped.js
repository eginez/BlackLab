const chai = require("chai");
const chaiHttp = require("chai-http");
const expect = chai.expect;
const should = chai.should();
chai.use(chaiHttp);

const constants = require('./constants');
const util = require('./util');

// Test that a hits search for a pattern returns the correct number of hits and docs,
// and optionally test that the first hit matches (either JSON or text).
function expectHitsGrouped(params, numberOfGroups, numberOfHits, numberOfDocs, expectedFirstGroupJson) {
    const pattern = params.patt;
    const groupBy = params.group;
    const filteredBy  = params.filter ? `, filtered by ${params.filter}` : '';

    describe(`/hits with pattern ${pattern}${filteredBy}, grouped by ${groupBy}`, () => {
        it('should return expected response (#groups/hits/docs, structure)', done => {
            chai
                .request(constants.SERVER_URL)
                .get('/test/hits')
                .query({
                    sort: "size,identity",
                    wordsaroundhit: 1,
                    ...params,
                })
                .set('Accept', 'application/json')
                .end((err, res) => {
                    expect(err).to.be.null;
                    expect(res).to.have.status(200);
                    const body = res.body;
                    expect(body).to.be.a("object").that.has.all.keys(
                        "summary",
                        "hitGroups"
                    );

                const numberOfResultsInResponse = Math.min(constants.DEFAULT_WINDOW_SIZE, numberOfGroups);

                // Sanity-check hits
                const hitGroups = body.hitGroups;
                expect(hitGroups).to.be.an("array").that.has.lengthOf(numberOfResultsInResponse);
                const group = hitGroups[0];
                if (expectedFirstGroupJson) {
                    expect(group, 'group').to.deep.equal(expectedFirstGroupJson);
                } else {
                    expect(group, 'group').to.be.an("object").that.has.all.keys(
                        'identity',
                        'identityDisplay',
                        'size',
                        'properties',
                        'numberOfDocs',
                    );
                }

                // Sanity-check summary
                const summary = body.summary;
                const expectLargestGroupSize = group.size;
                expect(summary).to.deep.include({
                    "searchParam": {
                        "indexname": "test",
                        "sort": "size,identity",
                        "wordsaroundhit": "1",
                        ...params
                    },
                    "windowFirstResult": 0,
                    "numberOfGroups": numberOfGroups,
                    "largestGroupSize": expectLargestGroupSize,
                    "requestedWindowSize": constants.DEFAULT_WINDOW_SIZE,
                    "actualWindowSize": numberOfResultsInResponse,
                    "windowHasPrevious": false,
                    "windowHasNext": numberOfGroups > constants.DEFAULT_WINDOW_SIZE,
                    "stillCounting": false,
                    "numberOfHits": numberOfHits,
                    "numberOfHitsRetrieved": numberOfHits,
                    "stoppedCountingHits": false,
                    "stoppedRetrievingHits": false,
                    "numberOfDocs": numberOfDocs,
                    "numberOfDocsRetrieved": numberOfDocs,
                });

                done();
            });
        });
    });
}


// Single word
expectHitsGrouped({ patt: '"very"', group: 'wordright:word:i' }, 6, 7, 2, {
    "identity": "cwo:word:i:much",
    "identityDisplay": "much",
    "size": 2,
    "properties": [
        {
            "name": "wordright:word:i",
            "value": "much"
        }
    ],
    "numberOfDocs": 2
});

expectHitsGrouped({ patt: '"a"', group: 'field:title' }, 3, 17, 3, {
    "identity": "str:interview about conference experience and impressions of city",
    "identityDisplay": "interview about conference experience and impressions of city",
    "size": 8,
    "properties": [
        {
            "name": "field:title",
            "value": "interview about conference experience and impressions of city"
        }
    ],
    "numberOfDocs": 1,
    "subcorpusSize": {
        "documents": 1,
        "tokens": 268
    }
});

// Compare hit grouping with regular (HitGroupFromHits) and fast (HitGroupsTokenFrequencies) path.
// Results should be identical
const param1 = [210, 766, 3, {
    "identity": "cws:word:i:_0",
    "identityDisplay": "_0",
    "size": 43,
    "properties": [
        {
            "name": "hit:word:i",
            "value": "_0"
        }
    ],
    "numberOfDocs": 3
}];
expectHitsGrouped({ patt: '[word != "abcdefg"]', group: 'hit:word:i'}, ...param1); // regular path
expectHitsGrouped({ patt: '[]', group: 'hit:word:i'}, ...param1); // fast path, same results expected

// Same comparison but with metadata filter
const param2 = [118, 334, 1, {
    "identity": "cws:word:i:_0",
    "identityDisplay": "_0",
    "size": 22,
    "properties": [
        {
            "name": "hit:word:i",
            "value": "_0"
        }
    ],
    "numberOfDocs": 1
}];
expectHitsGrouped({ patt: '[word != "abcdefg"]', filter: 'fromInputFile:"/input/PBsve430.xml"', group: 'hit:word:i'}, ...param2); // fast path, same results expected
expectHitsGrouped({ patt: '[]', filter: 'fromInputFile:"/input/PBsve430.xml"', group: 'hit:word:i'}, ...param2); // fast path, same results expected
