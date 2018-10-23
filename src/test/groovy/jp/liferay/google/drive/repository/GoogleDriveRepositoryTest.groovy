package jp.liferay.google.drive.repository

import spock.lang.Specification
import spock.lang.Unroll

class GoogleDriveRepositoryTest extends Specification {

    @Unroll("Stream test success")
    def "Stream test success"() {
        when:
        def list = new ArrayList<String>() {
            {
                add("1");
                add("2");
                add("3");
                add("4");
                add("5");
                add("6");
                add("7");
                add("8");
                add("9");
                add("10");
            }
        };
        GoogleDriveRepository gdr = new GoogleDriveRepository();
        def result = gdr.filterList(list, _delta, _maxSize,)

        then:
        result == _result;

        where:
        _delta | _maxSize | _result
        0      | 2        | ["1", "2"]
        4      | 2        | ["5", "6"]
        0      | 10       | ["1", "2", "3", "4", "5", "6", "7", "8", "9", "10"]
        10     | 2        | []
        11     | 0        | []
        0      | 11       | ["1", "2", "3", "4", "5", "6", "7", "8", "9", "10"]
        9      | 3        | ["10"]
        5      | 10       | ["6", "7", "8", "9", "10"]
    }
}
