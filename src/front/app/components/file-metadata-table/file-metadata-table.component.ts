/*
 * This file is part of MyDMAM.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * Copyright (C) Media ex Machina 2026
 * 
 */
import { Component, computed, inject, input } from '@angular/core';
import { AssetResponse } from '../../dto/asset-response.interface';
import { FileMetadataClassifier } from './FileMetadataClassifier';
import { FileMetadataKey } from './FileMetadataKey';
import { Utils } from '../../utils';
import { FileMetadataValue } from './FileMetadataValue';
import { Nl2brPipe } from '../../pipes/nl2br-pipe';
import { FirstUpperCasePipe } from '../../pipes/first-upper-case-pipe';
import { MtdThesaurusResolver } from '../../services/mtd-thesaurus-resolver.service';
import { SpanDateTimeComponent } from '../toolkit/span-date-time.component';

@Component({
    selector: 'app-file-metadata-table',
    imports: [Nl2brPipe, FirstUpperCasePipe, SpanDateTimeComponent],
    templateUrl: './file-metadata-table.component.html',
    styleUrl: './file-metadata-table.component.css',
})
export class FileMetadataTableComponent {


    readonly mtdThesaurusResolver = inject(MtdThesaurusResolver);
    readonly assetResponse = input.required<AssetResponse>();
    readonly classifiers = computed(this.getClassifiers.bind(this));
    readonly numberFormat = new window.Intl.NumberFormat();

    getRawValueByIndex(index: number, key: FileMetadataKey): string {
        return key.valueByIndex.filter(vi => vi.index == index).map(vi => vi.value).at(0) || "";
    }

    getValueByIndex(index: number, key: FileMetadataKey): string {
        const numberFormat = this.numberFormat;
        const value = this.getRawValueByIndex(index, key);

        const toNumber = function(unit:string|null, unitPlurial:string|null):string {
            if (value == "" || value.startsWith("0x")) {
                return value;
            }
            const valueNumber = Number(value);
            if (Number.isNaN(valueNumber) == false) {
                if (unit == "bps") {
                    return Utils.bpsToEngNotation(valueNumber);
                } else if (unit == "ms") {
                    return Utils.msToHMS(valueNumber);
                } else if (unitPlurial != null && valueNumber > 1) {
                    return numberFormat.format(valueNumber) + " " + unitPlurial;
                } else if (unit != null) {
                    return numberFormat.format(valueNumber) + " " + unit;
                }
                return numberFormat.format(valueNumber);
            }
            return value;
        };

        switch (key.signature?.unit) {
            case "MILLISECONDS":
                return toNumber("ms", null);
            case "MILLIMETERS":
                return toNumber("mm", null);
            case "DEGREES":
                return toNumber("°", null);
            case "TRACKS":
                return toNumber("track", "tracks");
            case "HERTZ":
                return toNumber("Hz", null);
            case "BITS_PER_SECONDS":
                return toNumber("bps", null);
            case "FRAMES_PER_SECONDS":
                return toNumber("fps", null);
            case "PIXELS":
                return toNumber("pixel", "pixels");
            case "NO_UNIT":
                return toNumber(null, null);
            default:
                console.warn("Please update front code from backend, not managed:", key.signature?.unit);
                return toNumber(null, null);
        }
    }

    getNumberValueByIndex(index: number, key: FileMetadataKey): number {
        const value = this.getRawValueByIndex(index, key);
        const valueNumber = Number(value);
        if (value == "" || Number.isNaN(valueNumber)) {
            return 0;
        }
        return valueNumber;
    }

    private getClassifiers(): Array<FileMetadataClassifier> {
        interface DbEntry {
            index: number;
            classifierName: string;
            key: string;
            value: string;
        }
        const allDbEntries: Array<DbEntry> = [];

        const allIndexes = this.assetResponse().index;
        for (const i in allIndexes) {
            const fileMetadatas = allIndexes[i].fileMetadatas;
            if (fileMetadatas != null) {
                fileMetadatas.forEach(fm => {
                    allDbEntries.push({
                        index: allIndexes[i].index,
                        classifierName: fm.classifier,
                        key: fm.key,
                        value: fm.value
                    });
                });
            }
        }
        
        const classifiersNames:Array<string> = [];
        allDbEntries.map(entry => entry.classifierName)
            .forEach(cN => classifiersNames.push(cN));
        Utils.distinct(classifiersNames);

        const result = classifiersNames.map(classifierName => {
            const currentClassifierDbEntries = allDbEntries.filter(entry => entry.classifierName === classifierName);
            const indexes = Utils.distinct(currentClassifierDbEntries.map(entry => entry.index));
            const keys: Array<FileMetadataKey> = [];
            const keysForClassifier = Utils.distinct(currentClassifierDbEntries.map(entry => entry.key));

            keysForClassifier.forEach(key => {
                const dbEntriesForClassifierKey = currentClassifierDbEntries.filter(entry => entry.key == key);
                const valueByIndex: Array<FileMetadataValue> = [];
                dbEntriesForClassifierKey.forEach(entry => {
                    valueByIndex.push({
                        index: entry.index,
                        value: entry.value,
                        track: classifierName + "." + key + "." + entry.index + "=" + entry.value
                    });
                });
                keys.push({
                    classifierName: classifierName,
                    name: key.replaceAll("-", " "),
                    signature: this.mtdThesaurusResolver.getEntrySignatureByKeyName(classifierName, key),
                    valueByIndex: valueByIndex,
                    track: classifierName + "." + key
                });
            });

            keys.sort((l,r) => {
                if (l.signature != null && r.signature != null) {
                    const lsort = l.signature.sortIndexOrder;
                    const rsort = r.signature.sortIndexOrder;
                    return lsort == rsort ? 0 : (lsort > rsort ? 1 : (lsort < rsort ? -1 : 0));
                }
                return l.track.localeCompare(r.track);
            });

            return {
                name: classifierName.replaceAll(":", " • "),
                signature: this.mtdThesaurusResolver.getClassifierSignature(classifierName),
                indexes: indexes,
                keys: keys,
                track: classifierName
            };
        });

        result.sort((l,r) => {
            if (l.signature != null && r.signature != null) {
                const lsort = l.signature.sortIndexOrder;
                const rsort = r.signature.sortIndexOrder;
                return lsort == rsort ? 0 : (lsort > rsort ? 1 : (lsort < rsort ? -1 : 0));
            }
            return l.track.localeCompare(r.track);
        });

        return result;
    }

}
