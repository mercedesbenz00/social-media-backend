package iq.earthlink.social.shortvideoregistryservice.dto;

import com.datastax.oss.protocol.internal.util.Bytes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.query.CassandraPageRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.nio.ByteBuffer;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageInfo {


    private String pagingState;
    private Integer size;


    public CassandraPageRequest getPageable() {
        if (this.size == null)
            this.size = 20;
        Pageable pageable = PageRequest.of(0, this.size);
        if (this.pagingState == null || this.pagingState.trim().equals(""))
            return CassandraPageRequest.first(pageable.getPageSize());
        ByteBuffer byteBuffer = null;
        try {
            byteBuffer = Bytes.fromHexString(this.pagingState);
        } catch (Exception e) {
            return CassandraPageRequest.first(pageable.getPageSize());
        }
        return CassandraPageRequest.of(pageable, byteBuffer);
    }

}
