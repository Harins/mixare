/*
 * Copyleft 2012 - Peer internet solutions 
 * 
 * This file is part of mixare.
 * 
 * This program is free software: you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version. 
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details. 
 * 
 * You should have received a copy of the GNU General Public License along with 
 * this program. If not, see <http://www.gnu.org/licenses/>
 */
package org.mixare.mgr.datasource;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.mixare.MixContext;
import org.mixare.data.DataSource;
import org.mixare.data.DataSourceStorage;
import org.mixare.mgr.downloader.DownloadRequest;

class DataSourceMgrImpl implements DataSourceManager {

	private final ConcurrentLinkedQueue<DataSource> allDataSources=new ConcurrentLinkedQueue<DataSource>(); 
	
	private final MixContext ctx;

	public DataSourceMgrImpl(MixContext ctx) {
		this.ctx = ctx;
	}

	@Override
	public boolean isAtLeastOneDatasourceSelected() {
		boolean atLeastOneDatasourceSelected = false;
		for (DataSource ds : this.allDataSources) {
			if (ds.getEnabled()){
				atLeastOneDatasourceSelected = true;
				break; //if condition met, break from loop.
			}
		}
		return atLeastOneDatasourceSelected;
	}



	public void setAllDataSourcesforLauncher(DataSource datasource) {
		this.allDataSources.clear(); // TODO WHY? CLEAN ALL
		this.allDataSources.add(datasource);
	}

	public void refreshDataSources() {
		this.allDataSources.clear();

		DataSourceStorage.getInstance(ctx).fillDefaultDataSources();

		int size = DataSourceStorage.getInstance().getSize();

		// copy the value from shared preference to adapter
		for (int i = 0; i < size; i++) {
			String fields[] = DataSourceStorage.getInstance().getFields(i);
			this.allDataSources.add(new DataSource(fields[0], fields[1],
					fields[2], fields[3], fields[4]));
		}
	}

	public void requestDataFromAllActiveDataSource(double lat, double lon,
			double alt, float radius) {
		for (DataSource ds : allDataSources) {
			/*
			 * when type is OpenStreetMap iterate the URL list and for selected
			 * URL send data request
			 */
			if (ds.getEnabled()) {
				requestData(ds, lat, lon, alt, radius, Locale.getDefault()
						.getLanguage());
			}
		}

	}

	private void requestData(DataSource datasource, double lat, double lon,
			double alt, float radius, String locale) {
		
		/* BLUR the lat and lon at the second decimal position. 
		 * It means roughly 250 meters.
		 * It depends on the position on earth and is slightly different for latitude
		 * and longitude, but should work well enough.
		 * 
		 * TODO: add a preference to let the user activate this behavior
		 */
		BigDecimal blurredLat = new BigDecimal(lat);
		blurredLat = blurredLat.setScale(2, BigDecimal.ROUND_HALF_EVEN);

		BigDecimal blurredLng = new BigDecimal(lon);
		blurredLng = blurredLng.setScale(2, BigDecimal.ROUND_HALF_EVEN);

		DownloadRequest request = new DownloadRequest(datasource,
				datasource.createRequestParams(blurredLat.doubleValue(), blurredLng.doubleValue(), alt, radius, locale));
		ctx.getDownloadManager().submitJob(request);

	}

}
