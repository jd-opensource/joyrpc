package io.joyrpc.codec.serialization.model;

/*-
 * #%L
 * joyrpc
 * %%
 * Copyright (C) 2019 joyrpc.io
 * %%
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
 * #L%
 */

import java.util.ArrayList;
import java.util.List;

public class AddressBook {

    private List<Person> people;

    public AddressBook() {
    }

    public AddressBook(Person... people) {
        this.people = new ArrayList(people == null ? 0 : people.length);
        if (people != null) {
            for (Person person : people) {
                this.people.add(person);
            }
        }
    }

    public List<Person> getPeople() {
        return people;
    }

    public void setPeople(List<Person> people) {
        this.people = people;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AddressBook that = (AddressBook) o;

        return people != null ? people.equals(that.people) : that.people == null;
    }

    @Override
    public int hashCode() {
        return people != null ? people.hashCode() : 0;
    }
}
